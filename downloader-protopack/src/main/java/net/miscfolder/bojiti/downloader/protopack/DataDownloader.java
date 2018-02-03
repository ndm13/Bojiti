package net.miscfolder.bojiti.downloader.protopack;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.SimpleURLConnectionDownloader;
import net.miscfolder.protopack.handlers.data.DataURLConnection;

@Protocols("data")
public class DataDownloader extends SimpleURLConnectionDownloader{
	public DataDownloader(){
		super(DataURLConnection.class, "data");
	}
}
