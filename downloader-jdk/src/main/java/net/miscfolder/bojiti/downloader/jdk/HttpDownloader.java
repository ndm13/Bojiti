package net.miscfolder.bojiti.downloader.jdk;

import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.downloader.URLConnectionDownloader;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;

@Protocols({"http","https"})
public class HttpDownloader extends URLConnectionDownloader{
	private static final System.Logger LOGGER = System.getLogger(HttpDownloader.class.getName());

	private static volatile boolean sslBypassed = false;
	private static final Object sslBypassLock = new Object();
	private static final SSLSocketFactory defaultSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
	private static final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

	private String userAgent;

	public HttpDownloader(){
		this.userAgent = "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0";
		installSSLBypass();
	}

	public static void installSSLBypass(){
		if(sslBypassed) return;
		synchronized(sslBypassLock){
			if(sslBypassed) return;
			TrustManager[] trustAll = new TrustManager[]{
					new X509TrustManager(){
						@Override
						public void checkClientTrusted(X509Certificate[] x509Certificates, String s){
						}

						public java.security.cert.X509Certificate[] getAcceptedIssuers(){
							return null;
						}

						public void checkServerTrusted(X509Certificate[] certs, String authType){
						}
					}
			};
			try{
				SSLContext context = SSLContext.getInstance("SSL");
				context.init(null, trustAll, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
			}catch(NoSuchAlgorithmException e){
				throw new IllegalStateException("SSL context not available", e);
			}catch(KeyManagementException e){
				throw new IllegalStateException("Paradox: Java SecureRandom not from Java", e);
			}
			HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
			sslBypassed = true;
		}
	}

	public static void removeSSLBypass(){
		if(!sslBypassed) return;
		synchronized(sslBypassLock){
			if(!sslBypassed) return;
			HttpsURLConnection.setDefaultSSLSocketFactory(defaultSocketFactory);
			HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
		}
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
