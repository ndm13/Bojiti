package net.miscfolder.bojiti.worker;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.internal.Announcer;
import net.miscfolder.bojiti.parser.Parser;

public class Worker implements Announcer<Worker.Listener>{
	private static final int MAX_CONCURRENT_CONNECTIONS = 16;
	private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CONNECTIONS);

	private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
	private final Iterator<URL> urlProvider;

	private ExecutorService
			monitorService = Executors.newSingleThreadExecutor(),
			downloaderService = Executors.newFixedThreadPool(MAX_CONCURRENT_CONNECTIONS),
			parserService = Executors.newCachedThreadPool();


	public Worker(Iterator<URL> urlProvider){
		this.urlProvider = Objects.requireNonNull(urlProvider);
	}

	@Override
	public Set<Listener> listeners(){
		return listeners;
	}

	public void start(){
		monitorService.submit(()->{
			while(!downloaderService.isShutdown()){
				try{
					semaphore.acquire();
					downloaderService.submit(()->{
						try{
							this.process();
						}catch(Exception e){
							e.printStackTrace();
						}
						// Must release
						semaphore.release();
					});
				}catch(InterruptedException e){
					break;
				}
			}
		});
	}

	public void process(){
		URL url;
		synchronized(urlProvider){
			if(!urlProvider.hasNext()) return;
			url = urlProvider.next();
		}
		Downloader downloader = SPI.Downloaders.getFirst(url.getProtocol());
		if(downloader != null){
			try{
				Response response = downloader.download(url);
				announce(l->l.onDownloadComplete(response));
				parserService.submit(()->{
					String type = response.getContentType().toLowerCase();
					int semicolon = type.indexOf(';');
					if(semicolon != -1) type = type.substring(0, semicolon);
					Parser parser = SPI.Parsers.getFirst(type);
					if(parser != null){
						Set<URI> uris = parser.parse(url, response.getContent());
						announce(l->l.onParsingComplete(url, uris));
					}
				});
			}catch(IOException e){
				announce(l->l.onWorkerError(url, e));
			}catch(RedirectionException e){
				announce(l->l.onParsingComplete(url, e.getTargets()));
			}
		}
	}

	public void timeout(long timeout, TimeUnit unit) throws InterruptedException{
		monitorService.shutdown();
		System.out.println("Stopped creating new workers");
		monitorService.awaitTermination(timeout, unit);
		downloaderService.shutdown();
		// DEBUG
		System.out.println("Stopped accepting new workers");
		downloaderService.awaitTermination(timeout, unit);
		parserService.shutdown();
		// DEBUG
		System.out.println("Stopped accepting new parsers");
		parserService.awaitTermination(timeout, unit);
	}


	public interface Listener{
		void onDownloadComplete(Response response);
		void onParsingComplete(URL host, Set<URI> urls);
		void onWorkerError(URL url, IOException exception);
	}

}
