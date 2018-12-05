package net.miscfolder.bojiti.crawler;

import net.miscfolder.bojiti.downloader.Response;

import java.net.URI;
import java.util.EventListener;

public interface DownloadEventListener extends EventListener{
	void onBegin(URI uri);
	void onUpdate(URI uri, Response.Progress progress);
	void onComplete(URI uri, Response response);
	void onFailure(URI uri);
}
