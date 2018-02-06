package net.miscfolder.bojiti.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.worker.Worker;

public class WorkerTest{
	private static final Set<URL> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static final TransferQueue<URL> queue = new LinkedTransferQueue<>();

	public static void main(String[] args) throws MalformedURLException, InterruptedException{
		Worker worker = new Worker(new QueuePoppingIterator());
		worker.addListener(new Worker.Listener(){
			@Override
			public void onDownloadComplete(Response response){
				System.out.println("Downloaded " + response.getURL().toExternalForm());
			}

			@Override
			public void onParsingComplete(URL host, Set<URL> urls){
				System.out.println("Parsed " + host.toExternalForm() + "\n\tfound " + urls.size());
				for(URL url : urls){
					if(set.add(url)){
						try{
							queue.transfer(url);
						}catch(InterruptedException ignore){}
					}
				}
			}

			@Override
			public void onWorkerError(URL url, IOException exception){
				synchronized(System.err){
					System.out.println("Error on " + url.toExternalForm());
					exception.printStackTrace();
				}
			}
		});

		URL start = new URL("http://www.google.com/");
		set.add(start);
		queue.put(start);

		worker.start();
		worker.timeout(5, TimeUnit.MINUTES);
	}

	static class QueuePoppingIterator implements Iterator<URL>{
		URL element = null;

		@Override
		public boolean hasNext(){
			if(element != null) return true;
			try{
				return (element = queue.poll(2, TimeUnit.MINUTES)) != null;
			}catch(InterruptedException e){
				return false;
			}
		}

		@Override
		public URL next(){
			hasNext();
			URL element = this.element;
			System.out.println("Took element " + element.toExternalForm());
			this.element = null;
			return element;
		}
	}
}
