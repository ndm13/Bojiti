package net.miscfolder.bojiti.downloader.protopack;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.SimpleURLConnectionDownloader;
import net.miscfolder.protopack.ProtoPack;
import net.miscfolder.protopack.handlers.gopher.GopherURLConnection;

@Protocols("gopher")
public class GopherDownloader extends SimpleURLConnectionDownloader{
	static{ProtoPack.install();}

	public GopherDownloader(){
		super(GopherURLConnection.class, "gopher");
	}
}
