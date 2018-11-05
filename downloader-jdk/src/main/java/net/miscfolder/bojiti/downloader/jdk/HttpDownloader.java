package net.miscfolder.bojiti.downloader.jdk;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.downloader.URLConnectionDownloader;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.function.Consumer;

@Protocols({"http","https"})
public class HttpDownloader extends URLConnectionDownloader{
	private static final System.Logger LOGGER = System.getLogger(HttpDownloader.class.getName());

	private String userAgent;

	public HttpDownloader(){
		this.userAgent = "Mozilla/5.0 (Windows NT 6.1; rv:57.0) Gecko/20100101 Firefox/57.0";
	}

	@Override
	public Response download(URL url, Consumer<Response.Progress> callback) throws IOException, RedirectionException{
		URLConnection interim = url.openConnection();
		if(!(interim instanceof HttpURLConnection))
			throw new IllegalArgumentException("URL isn't HTTP-compatible");

		HttpURLConnection connection = (HttpURLConnection) interim;

		// Set headers
		String userInfo = encodeUserInfo(url.getUserInfo());
		if(userInfo != null)
			connection.setRequestProperty("Authorization", "Basic " + userInfo);
		connection.setRequestProperty("User-Agent", userAgent);
		connection.setInstanceFollowRedirects(false);

		try{
			Response response = download(connection, connection.getInputStream(), callback);
			if(connection.getResponseCode() > 299 && connection.getResponseCode() < 400){
				throw new RedirectionException(response, connection.getResponseCode(),
						getRedirectTargets(connection));
			}
			return response;
		}catch(IOException exception){
			try{
				return download(connection, connection.getErrorStream(), callback);
			}catch(IOException | IllegalArgumentException ignore){
				throw exception;
			}
		}
	}

	public String getUserAgent(){
		return userAgent;
	}

	private static String encodeUserInfo(String userInfo){
		if(userInfo != null && userInfo.length() > 0)
			return Base64.getEncoder().encodeToString(userInfo.getBytes());
		return null;
	}

	private static Set<URI> getRedirectTargets(URLConnection connection){
		List<String> targetList = connection.getHeaderFields().get("location");
		if(targetList == null) targetList = connection.getHeaderFields().get("Location");
		if(targetList == null) return Collections.emptySet();

		Set<URI> targets = new HashSet<>();
		for(String target : targetList){
			try{
				targets.add(new URL(connection.getURL(), target).toURI());
			}catch(MalformedURLException | URISyntaxException e){
				LOGGER.log(System.Logger.Level.WARNING,
						()->"Could not resolve/convert URL: " + connection.getURL() + " -> " + target, e);
			}
		}
		return targets;
	}
}
