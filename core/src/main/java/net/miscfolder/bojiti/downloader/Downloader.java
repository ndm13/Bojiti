package net.miscfolder.bojiti.downloader;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.miscfolder.bojiti.internal.Announcer;

public abstract class Downloader implements Announcer<Downloader.Listener>{
	private Set<Listener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	public Set<Listener> listeners(){
		return listeners;
	}

	public abstract Response download(URL url) throws IOException, RedirectionException;

	public interface Listener{
		void onDownloadStarted(Response response);
		void onDownloadComplete(Response response);
		void onDownloadError(Response response, Exception exception);
	}
}
