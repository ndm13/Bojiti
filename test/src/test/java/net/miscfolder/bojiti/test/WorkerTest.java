package net.miscfolder.bojiti.test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import net.miscfolder.bojiti.test.backend.InMemoryBackend;
import net.miscfolder.bojiti.worker.Worker;
import net.miscfolder.protopack.ProtoPack;
import net.miscfolder.roxyproxy.RoxyProxy;
import net.miscfolder.roxyproxy.implementations.I2PProxyPlugin;
import net.miscfolder.roxyproxy.implementations.TorProxyPlugin;

public class WorkerTest{
	public static void main(String[] args) throws InterruptedException{
		ProtoPack.install();
		RoxyProxy.install(TorProxyPlugin.DEFAULT,
				I2PProxyPlugin.HTTP.DEFAULT, I2PProxyPlugin.HTTPS.DEFAULT);

		Scanner scanner = new Scanner(System.in);
		InMemoryBackend backend = new InMemoryBackend();
		System.out.println("Seed URLs:");
		int loaded = 0;
		do{
			try{
				String line = scanner.nextLine().trim();
				if(line.isEmpty())
					break;
				backend.add(new URL(line));
				System.out.print("\rLoaded " + ++loaded);
			}catch(MalformedURLException ignore){
				System.err.println("Malformed URL");
			}
		}while(true);
		scanner.close();

		Worker worker = backend.createWorker();

		worker.start();
		Thread.sleep(120000);
		worker.timeout(60, TimeUnit.SECONDS);

		Set<URI> knownGood = backend.getKnownGood();
		System.out.println("\nKnown Good: " + knownGood.size());
		System.out.println("= = = = = = = = =");
		new TreeSet<>(knownGood).stream().map(URI::toASCIIString).forEach(System.out::println);
	}
}
