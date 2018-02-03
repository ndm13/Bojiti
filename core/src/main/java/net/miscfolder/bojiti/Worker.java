package net.miscfolder.bojiti;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.parser.Parser;

public class Worker implements Runnable{
	public static final int CONCURRENT_CONNECTIONS = 16;

	private ForkJoinPool
			workerService = new ForkJoinPool(CONCURRENT_CONNECTIONS),
			parserService = new ForkJoinPool();

	private final Iterator<URL> urlProvider;
	private final Consumer<Response> responseConsumer;
	private final Consumer<Set<URL>> urlConsumer;

	public Worker(Iterator<URL> urlProvider, Consumer<Response> responseConsumer, Consumer<Set<URL>> urlConsumer){
		this.urlProvider = urlProvider;
		this.responseConsumer = responseConsumer;
		this.urlConsumer = urlConsumer;
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
				responseConsumer.accept(response);
				parserService.submit(()->{
					String type = response.getContentType().toLowerCase();
					int semicolon = type.indexOf(';');
					if(semicolon != -1) type = type.substring(0,semicolon);
					Parser parser = SPI.Parsers.getFirst(type);
					if(parser != null){
						Set<URL> urls = parser.parse(url, response.getContent());
						urlConsumer.accept(urls);
					}else{
						// TODO fix
						synchronized(System.out){System.err.println("ERROR: No parser for type: " + type);}
					}
				});
			}catch(IOException e){
				// TODO fix
				e.printStackTrace();
			}catch(RedirectionException e){
				urlConsumer.accept(e.getTargets());
			}
		}else{
			// TODO fix
			synchronized(System.out){System.err.println("ERROR: No downloader for protocol: " + url.getProtocol());}
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
}
