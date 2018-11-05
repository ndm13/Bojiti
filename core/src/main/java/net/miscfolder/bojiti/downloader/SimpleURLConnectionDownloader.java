package net.miscfolder.bojiti.downloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

public class SimpleURLConnectionDownloader extends URLConnectionDownloader{
	private final Class<? extends URLConnection> connectionType;
	private final String protocolName;

	public SimpleURLConnectionDownloader(Class<? extends URLConnection> connectionType,
			String protocolName){
		this.connectionType = connectionType;
		this.protocolName = protocolName;
	}

	@Override
	public Response download(URL url, Consumer<Response.Progress> callback) throws IOException{
		URLConnection interim = url.openConnection();
		if(!connectionType.isInstance(interim))
			throw new IllegalArgumentException("URL isn't " + protocolName +
					"-compatible, or resolver " + connectionType.getCanonicalName() +
					" is not available/default");
		return download(interim, interim.getInputStream(), callback);
	}
}
