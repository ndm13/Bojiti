package net.miscfolder.bojiti.test.support.minidns;

import org.minidns.AbstractDnsClient;
import org.minidns.dnsmessage.DnsMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class OpenNICDnsClient extends AbstractDnsClient{
	private static final System.Logger LOGGER = System.getLogger(OpenNICDnsClient.class.getCanonicalName());

	private static final List<InetAddress> V4_SERVERS = new ArrayList<>();
	private static final List<InetAddress> V6_SERVERS = new ArrayList<>();
	static{ loadServers(); }

	private static void loadServers(){
		try{
			URL v4API = new URL("https://api.opennicproject.org/geoip/?bare&anon=true&res=10&pct=95&ipv=4");
			URL v6API = new URL("https://api.opennicproject.org/geoip/?bard&anon=true&res=10&pct=95&ipv=6");
			try{
				Scanner sc = new Scanner(v4API.openStream());
				while(sc.hasNextLine()){
					try{
						V4_SERVERS.add(InetAddress.getByName(sc.nextLine()));
					}catch(UnknownHostException ignore){}
				}
			}catch(IOException e){
				LOGGER.log(System.Logger.Level.WARNING, "OpenNIC API inaccessible: v4 services offline", e);
			}
			try{
				Scanner sc = new Scanner(v6API.openStream());
				while(sc.hasNextLine()){
					try{
						V6_SERVERS.add(InetAddress.getByName('[' + sc.nextLine() + ']'));
					}catch(UnknownHostException ignore){}
				}
			}catch(IOException e){
				LOGGER.log(System.Logger.Level.WARNING, "OpenNIC API inaccessible: v6 services offline", e);
			}
		}catch(MalformedURLException e){
			LOGGER.log(System.Logger.Level.WARNING, "OpenNIC API inaccessible: URL unresolvable", e);
		}
		try{
			if(V4_SERVERS.isEmpty()){
				V4_SERVERS.add(InetAddress.getByName("185.121.177.177"));
				V4_SERVERS.add(InetAddress.getByName("169.239.202.202"));
			}
			if(V6_SERVERS.isEmpty()){
				V6_SERVERS.add(InetAddress.getByName("[2a05:dfc7:5::53]"));
				V6_SERVERS.add(InetAddress.getByName("[2a05:dfc7:5::5353]"));
			}
		}catch(UnknownHostException e){
			LOGGER.log(System.Logger.Level.WARNING, "Static anycast server invalid", e);
		}
	}

	public List<InetAddress> getServerAddresses(){
		List<InetAddress> servers = new ArrayList<>();
		Collections.shuffle(V4_SERVERS);
		Collections.shuffle(V6_SERVERS);
		switch(getPreferedIpVersion()){
			case v6v4:
				servers.addAll(V6_SERVERS);
			case v4only:
				servers.addAll(V4_SERVERS);
				break;
			case v4v6:
				servers.addAll(V4_SERVERS);
			case v6only:
				servers.addAll(V6_SERVERS);
				break;
		}
		return servers;
	}

	@Override
	protected DnsMessage query(DnsMessage.Builder query) throws IOException{
		DnsMessage q = newQuestion(query).build();
		DnsMessage response = cache.get(q);
		if(response != null) return response;
		for(InetAddress dns : getServerAddresses()){
			try{
				return query(q, dns);
			}catch(IOException ignore){}
		}
		throw new IOException("All servers offline!  Check network connection.");
	}

	@Override
	protected DnsMessage.Builder newQuestion(DnsMessage.Builder questionMessage){
		questionMessage.setRecursionDesired(true);
		questionMessage.getEdnsBuilder().setUdpPayloadSize(dataSource.getUdpPayloadSize()).setDnssecOk();
		return questionMessage;
	}
}
