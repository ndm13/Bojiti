package net.miscfolder.bojiti.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
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
				if(++loaded % 10 == 0)
					System.out.print("\rLoaded " + loaded);
			}catch(MalformedURLException ignore){
				System.err.println("Malformed URL");
			}
		}while(true);
		scanner.close();

		Worker worker = new Worker(backend);
		worker.addListener(backend);

		worker.start();
		worker.timeout(5, TimeUnit.MINUTES);

		System.out.println("\nDiscovered\n==========");
		backend.getDiscovered().forEach(System.out::println);
	}
}
