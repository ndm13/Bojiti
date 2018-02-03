package net.miscfolder.bojiti.downloader;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Downloader{
	private Set<Listener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public abstract Response download(URL url) throws IOException, RedirectionException;

	public void addDownloadListener(Listener listener){
		listeners.add(listener);
	}
	public void removeDownloadListener(Listener listener){
		listeners.remove(listener);
	}
	protected void onDownloadStarted(Response response){
		listeners.forEach(l->l.onDownloadStarted(response));
	}
	protected void onDownloadComplete(Response response){
		listeners.forEach(l->l.onDownloadComplete(response));
	}
	protected void onDownloadError(Response response, Exception exception){
		listeners.forEach(l->l.onDownloadError(response, exception));
	}

	public interface Listener{
		void onDownloadStarted(Response response);
		void onDownloadComplete(Response response);
		void onDownloadError(Response response, Exception exception);
	}
}
