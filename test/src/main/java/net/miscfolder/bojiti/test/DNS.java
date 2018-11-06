package net.miscfolder.bojiti.test;

import java.net.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DNS{
	public static void main(String[] args) throws URISyntaxException{
		URI uri = new URI("http://google.com");
		long start = System.nanoTime();
		System.out.println(shouldTry(uri));
		System.out.println("Ran in " + (System.nanoTime() - start) + "ns");
	}

	private static Set<String> goodHosts = ConcurrentHashMap.newKeySet();
	private static Set<String> badHosts = ConcurrentHashMap.newKeySet();

	public static void addBadHost(String host){
		badHosts.add(host);
	}

	public static boolean shouldTry(URI uri){
		if(uri == null) return false;
		if(uri.getHost() != null){
			if(goodHosts.contains(uri.getHost())) return true;
			if(badHosts.contains(uri.getHost())) return false;
		}

		try{
			URL url = uri.toURL();
			if(url.getDefaultPort() == -1) return true; // Custom in-memory protocol
		}catch(MalformedURLException e){
			return false; // A non-convertible URL will fail down the line
		}

		if(uri.getHost() == null) return false; // Will break other things

		if(uri.getScheme() != null){
			List<Proxy> proxies = ProxySelector.getDefault().select(uri);
			if(proxies != null && !proxies.isEmpty() && proxies.get(0).type() != Proxy.Type.DIRECT){
				goodHosts.add(uri.getHost());
				return true; // We should try it because proxies may resolve it themselves
			}
		}

		// TODO FUTURE
		// There's a good chance that most of the DNS errors we get are from bad parsing decisions
		// (e.g. http://index.html).  If we can look up the root server for the domain (com, html),
		// then we can blacklist the bad ones and save ourselves lots of lookups!

		try{
			if(InetAddress.getAllByName(uri.getHost()).length > 0){
				goodHosts.add(uri.getHost());
				return true;
			}
		}catch(UnknownHostException ignore){}
		return false;
	}
}
