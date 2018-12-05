package net.miscfolder.bojiti.test.mvc;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.bojiti.test.support.DNS;
import net.miscfolder.bojiti.test.support.TimeoutSet;
import net.miscfolder.protopack.ProtoPack;
import net.miscfolder.roxyproxy.RoxyProxy;
import net.miscfolder.roxyproxy.implementations.TorProxyPlugin;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.EventListener;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Controller implements Runnable{
	private static final int DOWNLOADING_THREADS = Runtime.getRuntime().availableProcessors() * 3;
	private static final int PARSING_THREADS = Runtime.getRuntime().availableProcessors() * 6;
	private static final int ASYNC_THREADS = Runtime.getRuntime().availableProcessors() * 2;

	private final ExecutorService downloadService = Executors.newWorkStealingPool(DOWNLOADING_THREADS);
	private final ExecutorService parseService = Executors.newWorkStealingPool(PARSING_THREADS);
	private final ExecutorService asyncEventService = Executors.newWorkStealingPool(ASYNC_THREADS);

	private final Semaphore concurrentDownloads = new Semaphore(DOWNLOADING_THREADS);

	private final BlockingQueue<URI> queue = new LinkedBlockingDeque<>();
	private final Set<URI> checked = ConcurrentHashMap.newKeySet();
	private final Set<String> delayedHosts = new TimeoutSet<>(ConcurrentHashMap::new, Duration.ofMillis(200));
	private final Set<DownloadEventListener> downloadEventListeners = ConcurrentHashMap.newKeySet();
	private final Set<ParseEventListener> parseEventListeners = ConcurrentHashMap.newKeySet();

	private final AtomicInteger downloading = new AtomicInteger(0);
	private final AtomicInteger parsing = new AtomicInteger(0);

	private final Object startingLock = new Object();

	private Thread daemon;
	private volatile boolean started = false;

	Controller(){
		ProtoPack.install();
		RoxyProxy.install(TorProxyPlugin.DEFAULT);
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			synchronized(System.err){
				System.err.println(t.getName() + " threw " + e.getClass().getName() + ": " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		});
	}

	public void add(URI uri){
		queue.add(uri);
	}

	public void addDownloadListener(DownloadEventListener listener){
		downloadEventListeners.add(listener);
	}

	public void addParseListener(ParseEventListener listener){
		parseEventListeners.add(listener);
	}

	public void removeDownloadListener(DownloadEventListener listener){
		downloadEventListeners.remove(listener);
	}

	public void removeParseListener(ParseEventListener listener){
		parseEventListeners.remove(listener);
	}

	public boolean isStarted(){
		synchronized(startingLock){
			return started;
		}
	}

	public void start(){
		if(started) return;
		synchronized(startingLock){
			if(started) return;
			daemon = new Thread(this);
			daemon.setName("Controller Daemon");
			daemon.setDaemon(true);
			daemon.start();
			started = true;
		}
	}

	public void stop() throws InterruptedException{
		if(!started) return;
		synchronized(startingLock){
			if(!started) return;
			if(daemon != null && daemon.isAlive()){
				daemon.interrupt();
				daemon.join();
			}
			daemon = null;
			started = false;
		}
	}

	public void run(){
		while(!Thread.currentThread().isInterrupted()){
			try{
				concurrentDownloads.acquire();
				URI uri = queue.take();
				if(!delayedHosts.add(uri.getHost())){
					concurrentDownloads.release();
					queue.put(uri);
					continue;
				}
				final URL url = uri.toURL();
				downloadService.execute(new DownloadJob(uri, url));
			}catch(InterruptedException e){
				return;
			}catch(MalformedURLException ignore){
			}
		}
	}

	public interface DownloadEventListener extends EventListener{
		void onBegin(URI uri);
		void onUpdate(URI uri, Response.Progress progress);
		void onComplete(URI uri, Response response);
		void onFailure(URI uri);
	}
	public interface ParseEventListener extends EventListener{
		void onBegin(URI uri);
		void onUpdate(URI uri, int count);
		void onException(URI uri, ParserException e);
		void onComplete(URI uri, int count);
	}

	private class DownloadJob implements Runnable{
		private final URI uri;
		private final URL url;

		DownloadJob(URI uri, URL url){
			this.uri = uri;
			this.url = url;
		}

		@Override
		public void run(){
			try{
				downloading.incrementAndGet();
				if(Thread.currentThread().isInterrupted()) return;

				Downloader downloader = Downloader.SPI.getFirst(uri.getScheme());
				if(Thread.currentThread().isInterrupted()) return;

				asyncEventService.submit(() -> downloadEventListeners.forEach(l -> l.onBegin(uri)));
				Response response = downloader.download(url,
						p -> asyncEventService.submit(() -> downloadEventListeners.forEach(l -> l.onUpdate(uri, p))));
				asyncEventService.submit(() -> downloadEventListeners.forEach(l -> l.onComplete(uri, response)));

				parseService.submit(new ParseJob(response, uri));
			}catch(RedirectionException e){
				asyncEventService.submit(() -> downloadEventListeners.forEach(l -> l.onComplete(uri, e.getResponse())));
				e.getTargets().parallelStream().filter(DNS::shouldTry).filter(checked::add).forEach(queue::add);
			}catch(IOException | NoSuchElementException e){
				asyncEventService.submit(() -> downloadEventListeners.forEach(l -> l.onFailure(uri)));
				if(e instanceof UnknownHostException) DNS.addBadHost(url.getHost());
			}finally{
				downloading.decrementAndGet();
				concurrentDownloads.release();
			}
		}
	}

	private class ParseJob implements Runnable{
		private final Response response;
		private final URI uri;

		ParseJob(Response response, URI uri){
			this.response = Objects.requireNonNull(response);
			this.uri = Objects.requireNonNull(uri);
		}

		@Override
		public void run(){
			try{
				parsing.incrementAndGet();
				asyncEventService.submit(() -> parseEventListeners.forEach(l -> l.onBegin(uri)));
				Parser parser = Parser.SPI.getFirst(response.getBasicContentType());
				if(Thread.currentThread().isInterrupted()) return;

				Set<URI> uris = parser.parse(response.getURL(),
						response.getContent(),
						e -> asyncEventService.submit(() -> parseEventListeners.forEach(l -> l.onException(uri, e))),
						i -> asyncEventService.submit(() -> parseEventListeners.forEach(l -> l.onUpdate(uri, i))));
				response.clearContent();
				AtomicInteger size = new AtomicInteger(uris.size());
				for(URI u : uris){
					final URI clean = cleanup(u);
					if(DNS.shouldTry(clean) && checked.add(clean)){
						queue.add(clean);
					}else{
						parseEventListeners.forEach(l->l.onUpdate(uri, size.decrementAndGet()));
					}
				}
				asyncEventService.submit(() -> parseEventListeners.forEach(l -> l.onComplete(uri, uris.size())));
			}catch(NoSuchElementException ignore){
				response.clearContent();
				asyncEventService.submit(() -> parseEventListeners.forEach(l -> l.onComplete(uri, 0)));
			}finally{
				parsing.decrementAndGet();
			}
		}

		private URI cleanup(URI uri){
			try{
				URL url = uri.toURL();
				boolean negative = url.getPort() == url.getDefaultPort();
				if(negative || url.getRef() != null){
					return new URI(uri.getScheme(), uri.getRawUserInfo(), uri.getHost().toLowerCase(), negative ? -1 : uri.getPort(),
							uri.getRawPath(), uri.getRawQuery(), null);
				}
				return uri;
			}catch(MalformedURLException | URISyntaxException e){
				return null; // not resolvable
			}
		}
	}
}
