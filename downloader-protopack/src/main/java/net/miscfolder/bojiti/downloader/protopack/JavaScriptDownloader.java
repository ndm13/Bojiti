package net.miscfolder.bojiti.downloader.protopack;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.SimpleURLConnectionDownloader;
import net.miscfolder.protopack.ProtoPack;
import net.miscfolder.protopack.handlers.javascript.JavaScriptURLConnection;

@Protocols("javascript")
public class JavaScriptDownloader extends SimpleURLConnectionDownloader{
	static{ProtoPack.install();}

	public JavaScriptDownloader(){
		super(JavaScriptURLConnection.class, "javascript");
	}
}
