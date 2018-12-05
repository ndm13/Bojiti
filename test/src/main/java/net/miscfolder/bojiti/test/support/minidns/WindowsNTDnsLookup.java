package net.miscfolder.bojiti.test.support.minidns;

import org.minidns.dnsserverlookup.AbstractDnsServerLookupMechanism;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WindowsNTDnsLookup extends AbstractDnsServerLookupMechanism{
	private static List<String> cache;
	private static final Object lock = new Object();

	protected WindowsNTDnsLookup(int priority){
		super(WindowsNTDnsLookup.class.getSimpleName(), priority);
	}

	@Override
	public boolean isAvailable(){
		String operatingSystem = System.getProperty("os.name");
		return operatingSystem != null && operatingSystem.contains("Windows") &&
				!operatingSystem.contains("95") &&
				!operatingSystem.contains("98") &&
				!operatingSystem.contains("ME");
	}

	@Override
	public List<String> getDnsServerAddresses(){
		if(cache != null) return cache;
		synchronized(lock){
			if(cache != null) return cache;
			try{
				Process process = Runtime.getRuntime().exec("cmd /start cmd.exe");
				InputStream in = process.getInputStream();
				OutputStream out = process.getOutputStream();
				out.write("echo exit | nslookup\r\nexit\r\n".getBytes(StandardCharsets.US_ASCII));
				out.flush();
				out.close();
				Scanner scanner = new Scanner(in);
				Set<String> results = new HashSet<>();
				try{
					// Program launch, declaration, version, and prompt
					scanner.nextLine();
					scanner.nextLine();
					scanner.nextLine();
					scanner.nextLine();
				}catch(NoSuchElementException ignore){}
				while(scanner.hasNextLine()){
					String line = scanner.nextLine();
					if(line.isEmpty()) continue;
					if(line.contains(">")) continue; // prompt
					if(line.contains(":")){
						String host = line.split(":", 2)[1].trim();
						try{
							results.add(InetAddress.getByName(host).getHostAddress());
						}catch(UnknownHostException ignore){}
					}
				}
				in.close();
				process.destroy();
				return cache = new ArrayList<>(results);
			}catch(IOException e){
				return cache = List.of();
			}
		}


	}
}
