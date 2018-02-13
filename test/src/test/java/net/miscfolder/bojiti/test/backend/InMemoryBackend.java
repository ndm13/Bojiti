package net.miscfolder.bojiti.test.backend;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import net.miscfolder.bojiti.downloader.Response;

public class InMemoryBackend implements Backend{
	private final Set<URI> discovered = ConcurrentHashMap.newKeySet();
	private final TransferQueue<URI> queue = new LinkedTransferQueue<>();

	@Override
	public void add(URL... urls){
		for(URL url : urls){
			try{
				URI uri = url.toURI();
				if(discovered.add(uri)) queue.add(uri);
			}catch(URISyntaxException e){
				// DEBUG
				e.printStackTrace();
			}
		}
	}

	private URI element = null;

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
		do{
			hasNext();
			URI element = this.element;
			System.out.println(Thread.currentThread().getName() +
					" \tTook element " + element.toASCIIString());
			this.element = null;
			try{
				return element.toURL();
			}catch(MalformedURLException e){
				// DEBUG
				e.printStackTrace();
			}
		}while(true);
	}

	public Set<URI> getDiscovered(){
		return Collections.unmodifiableSet(discovered);
	}

	@Override
	public void onDownloadComplete(Response response){
		System.out.println(Thread.currentThread().getName() +
				" \tDownloaded " + response.getURL().toExternalForm());
	}

	@Override
	public void onParsingComplete(URL host, Set<URI> uris){
		System.out.println(Thread.currentThread().getName() +
				" \tParsed " + uris.size() + " from " + host.toExternalForm());
		for(URI uri : uris){
			if(discovered.add(uri)){
				try{
					queue.transfer(uri);
				}catch(InterruptedException ignore){
					System.err.println(Thread.currentThread().getName() +
							" \tInterrupted while transferring " + uri.toASCIIString());
				}
			}
		}
	}

	@Override
	public void onWorkerError(URL url, IOException exception){
		synchronized(System.err){
			System.out.println(Thread.currentThread().getName() +
					" \tError on " + url.toExternalForm());
			exception.printStackTrace();
		}
	}
}
