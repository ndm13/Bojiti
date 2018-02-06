package net.miscfolder.bojiti.downloader.jdk;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import net.miscfolder.bojiti.downloader.*;

@Protocols({"http","https"})
public class HttpDownloader extends URLConnectionDownloader{
	private String userAgent;

	public HttpDownloader(){
		this.userAgent = "Mozilla/5.0 (Windows NT 6.1; rv:57.0) Gecko/20100101 Firefox/57.0";
	}

	@Override
	public Response download(URL url) throws IOException, RedirectionException{
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
			try{
				Response response = download(connection, connection.getInputStream());
				if(connection.getResponseCode() > 299 && connection.getResponseCode() < 400){
					RedirectionException exception = new RedirectionException(
							connection.getURL(),
							connection.getResponseCode(),
							getRedirectTargets(connection));
					announce(l->l.onDownloadError(response, exception));
					throw exception;
				}
				announce(l->l.onDownloadComplete(response));
				return response;
			}catch(IOException exception){
				Response response = download(connection, connection.getErrorStream());
				announce(l->l.onDownloadComplete(response));
				return response;
			}
		}catch(NoSuchAlgorithmException exception){
			throw new IllegalStateException(exception);
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

	private static Set<URL> getRedirectTargets(URLConnection connection){
		List<String> targetList = connection.getHeaderFields().get("location");
		if(targetList == null) targetList = connection.getHeaderFields().get("Location");
		if(targetList == null) return Collections.emptySet();
		Set<URL> targets = new HashSet<>();
		for(String target : targetList){
			try{
				targets.add(new URL(connection.getURL(), target));
			}catch(MalformedURLException ignore){}
		}
		return targets;
	}
}
