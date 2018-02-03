package net.miscfolder.bojiti.downloader.protopack;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.SimpleURLConnectionDownloader;
import net.miscfolder.protopack.handlers.gopher.GopherURLConnection;

@Protocols("gopher")
public class GopherDownloader extends SimpleURLConnectionDownloader{
	public GopherDownloader(){
		super(GopherURLConnection.class, "gopher");
	}
}
