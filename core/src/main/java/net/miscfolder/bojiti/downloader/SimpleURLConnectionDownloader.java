package net.miscfolder.bojiti.downloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;

public class SimpleURLConnectionDownloader extends URLConnectionDownloader{
	private final Class<? extends URLConnection> connectionType;
	private final String protocolName;

	public SimpleURLConnectionDownloader(Class<? extends URLConnection> connectionType,
			String protocolName){
		this.connectionType = connectionType;
		this.protocolName = protocolName;
	}

	@Override
	public Response download(URL url) throws IOException{
		URLConnection interim = url.openConnection();
		if(!connectionType.isInstance(interim))
			throw new IllegalArgumentException("URL isn't " + protocolName +
					"-compatible, or resolver " + connectionType.getCanonicalName() +
					" is not available/default");
		Response response = new Response(interim);
		try{
			download(response, interim, interim.getInputStream());
		}catch(IOException | NoSuchAlgorithmException e){
			response.dispatch(l->l.onError(e));
		}
		return response;
	}
}
