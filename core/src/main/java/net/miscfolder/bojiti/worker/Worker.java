package net.miscfolder.bojiti.worker;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.internal.Announcer;
import net.miscfolder.bojiti.parser.Parser;

public class Worker implements Runnable, Announcer<Worker.Listener>{
	private static final int CONCURRENT_CONNECTIONS = 16;

	private final Set<Listener> listeners =
			Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Iterator<URL> urlProvider;

	private ForkJoinPool
			workerService = new ForkJoinPool(CONCURRENT_CONNECTIONS),
			parserService = new ForkJoinPool();


	public Worker(Iterator<URL> urlProvider){
		this.urlProvider = urlProvider;
	}

	@Override
	public Set<Listener> listeners(){
		return listeners;
	}

	public void start(){
		for(int i=0;i<CONCURRENT_CONNECTIONS;i++)
			workerService.submit(this);
	}

	@Override
	public void run(){
		URL url;
		synchronized(urlProvider){
			if(urlProvider.hasNext()){
				url = urlProvider.next();
			}else{
				return;
			}
		}
		Downloader downloader = SPI.Downloaders.getFirst(url.getProtocol());
		if(downloader != null){
			try{
				Response response = downloader.download(url);
				announce(l->l.onDownloadComplete(response));
				parserService.submit(()->{
					String type = response.getContentType().toLowerCase();
					int semicolon = type.indexOf(';');
					if(semicolon != -1) type = type.substring(0,semicolon);
					Parser parser = SPI.Parsers.getFirst(type);
					if(parser != null){
						Set<URL> urls = parser.parse(url, response.getContent());
						announce(l->l.onParsingComplete(url, urls));
					}
				});
			}catch(IOException e){
				announce(l->l.onWorkerError(url, e));
			}catch(RedirectionException e){
				announce(l->l.onParsingComplete(url, e.getTargets()));
			}
		}
		if(!Thread.currentThread().isInterrupted()){
			workerService.submit(this);
		}
	}

	public void timeout(long timeout, TimeUnit unit) throws InterruptedException{
		workerService.shutdown();
		workerService.awaitTermination(timeout, unit);
		parserService.shutdown();
		parserService.awaitTermination(timeout, unit);
	}

	public interface Listener{
		void onDownloadComplete(Response response);
		void onParsingComplete(URL host, Set<URL> urls);
		void onWorkerError(URL url, IOException exception);
	}
}
