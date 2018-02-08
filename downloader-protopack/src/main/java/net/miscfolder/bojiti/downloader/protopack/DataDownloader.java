package net.miscfolder.bojiti.downloader.protopack;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.SimpleURLConnectionDownloader;
import net.miscfolder.protopack.ProtoPack;
import net.miscfolder.protopack.handlers.data.DataURLConnection;

@Protocols("data")
public class DataDownloader extends SimpleURLConnectionDownloader{
	static{ProtoPack.install();}

	public DataDownloader(){
		super(DataURLConnection.class, "data");
	}
}
