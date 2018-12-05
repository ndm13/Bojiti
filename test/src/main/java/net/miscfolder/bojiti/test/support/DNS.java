package net.miscfolder.bojiti.test.support;

import net.miscfolder.bojiti.test.support.minidns.MiniDNS;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DNS{
	private static final System.Logger LOGGER = System.getLogger(DNS.class.getCanonicalName());
	public static void main(String[] args) throws URISyntaxException{
		URI uri = new URI("http://google.com");
		long start = System.nanoTime();
		System.out.println(shouldTry(uri));
		System.out.println("Ran in " + (System.nanoTime() - start) + "ns");
	}

	private static Set<String> goodHosts = ConcurrentHashMap.newKeySet();
	private static Set<String> badHosts = new TimeoutSet<>(ConcurrentHashMap::new, Duration.ofMinutes(60));
	// Don't time out hosts manually specified as bad
	private static Set<String> reallyBadHosts = ConcurrentHashMap.newKeySet();
	// We're unlikely to have a new TLD come online during the run
	private static Set<String> badRoots = ConcurrentHashMap.newKeySet();
	// and unlikely to have a TLD go offline during this time
	private static Set<String> goodRoots = ConcurrentHashMap.newKeySet();

	public static void addBadHost(String host){
		reallyBadHosts.add(host);
	}

	public static boolean shouldTry(URI uri){
		if(uri == null) return false;
		if(uri.getHost() != null){
			if(goodHosts.contains(uri.getHost())) return true;
			if(reallyBadHosts.contains(uri.getHost())) return false;
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

		// There's a good chance that most of the DNS errors we get are from bad parsing decisions
		// (e.g. http://index.html).  If we can look up the root server for the domain (com, html),
		// then we can blacklist the bad ones and save ourselves lots of lookups!
		String root = uri.getHost().substring(uri.getHost().lastIndexOf('.') + 1);
		if(badRoots.contains(root)) return false;
		try{
			if(!goodRoots.contains(root)){
				if(!MiniDNS.isValidRoot(root)){
					badRoots.add(root);
					return false;
				}else{
					goodRoots.add(root);
				}
			}

			// Speedup: if we're allowed to use MiniDNS, skip native maze
			if(MiniDNS.isValidInternet(uri.getHost())){
				goodHosts.add(uri.getHost());
				return true;
			}else{
				badHosts.add(uri.getHost());
				return false;
			}
		}catch(IOException e){
			LOGGER.log(System.Logger.Level.WARNING, "MiniDNS offline or malfunctioning", e);
		}

		try{
			if(InetAddress.getAllByName(uri.getHost()).length > 0){
				goodHosts.add(uri.getHost());
				return true;
			}
		}catch(UnknownHostException ignore){}
		badHosts.add(uri.getHost());
		return false;
	}
}
