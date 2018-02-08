package net.miscfolder.bojiti.test.backend;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import net.miscfolder.bojiti.downloader.Response;

public class InMemoryBackend implements Backend{
	private final Set<URL> discovered = ConcurrentHashMap.newKeySet();
	private final TransferQueue<URL> queue = new LinkedTransferQueue<>();

	private URL element = null;

	@Override
	public void add(URL... urls){
		for(URL url : urls)
			if(discovered.add(url))
				queue.add(url);
	}

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

	public Set<URL> getDiscovered(){
		return Collections.unmodifiableSet(discovered);
	}

	@Override
	public void onDownloadComplete(Response response){
		System.out.println("Downloaded " + response.getURL().toExternalForm());
	}

	@Override
	public void onParsingComplete(URL host, Set<URL> urls){
		System.out.println("Parsed " + urls.size() + " from " + host.toExternalForm());
		for(URL url : urls){
			if(discovered.add(url)){
				try{
					queue.transfer(url);
				}catch(InterruptedException ignore){
					System.err.println("Interrupted while transferring " + url.toExternalForm());
				}
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
}
