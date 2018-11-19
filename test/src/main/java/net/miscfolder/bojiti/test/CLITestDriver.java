package net.miscfolder.bojiti.test;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.test.support.DNS;
import net.miscfolder.protopack.ProtoPack;
import net.miscfolder.roxyproxy.RoxyProxy;
import net.miscfolder.roxyproxy.implementations.TorProxyPlugin;

import java.io.IOException;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CLITestDriver{
	private static final System.Logger LOGGER = System.getLogger(CLITestDriver.class.getName());
	public static void main(String[] args) throws URISyntaxException, MalformedURLException, InterruptedException{
		Logger.getGlobal().setLevel(Level.WARNING);
		ProtoPack.install();
		RoxyProxy.install(TorProxyPlugin.DEFAULT);
		ExecutorService service = Executors.newWorkStealingPool();

		BlockingDeque<URI> queue = new LinkedBlockingDeque<>();
		Set<URI> checked = ConcurrentHashMap.newKeySet();
		Set<URI> loadable = ConcurrentHashMap.newKeySet();
		Set<String> unloadable = ConcurrentHashMap.newKeySet();
		Parser.SPI.setLoggingMisses(true);
		Set<String> unparsable = ConcurrentHashMap.newKeySet();
		AtomicInteger parsed = new AtomicInteger();

		Scanner sc = new Scanner(System.in);
		queue.add(new URL(sc.nextLine()).toURI());
		sc.close();

		URI current;
		while(parsed.get() < 10 && null != (current = queue.pollFirst(600, TimeUnit.SECONDS))){
			URI finalCurrent = current;
			checked.add(finalCurrent);
			service.submit(()-> {
				try{
					Response response = Downloader.SPI.getFirst(finalCurrent.getScheme()).download(finalCurrent.toURL(),
							System.out::println);
					loadable.add(finalCurrent);
					LOGGER.log(System.Logger.Level.INFO, () -> "Downloaded URL " + finalCurrent.toASCIIString());
					try{
						Parser.SPI.getFirst(response.getBasicContentType())
								.parse(response.getURL(), response.getContent(),
										x->LOGGER.log(System.Logger.Level.WARNING, x))
								.stream()
								.filter(Objects::nonNull)
								.filter(u -> !checked.contains(u))
								.filter(DNS::shouldTry)
								.forEach(queue::addLast);
						parsed.incrementAndGet();
					}catch(NoSuchElementException e){
						unparsable.add(response.getBasicContentType());
					}
				}catch(IOException e){
					if(e instanceof UnknownHostException) DNS.addBadHost(finalCurrent.getHost());
					LOGGER.log(System.Logger.Level.WARNING, () -> "Exception downloading URL " + finalCurrent.toASCIIString(), e);
				}catch(RedirectionException e){
					LOGGER.log(System.Logger.Level.INFO, ()->"Found redirect at " + finalCurrent.toASCIIString(), e);
					queue.addAll(e.getTargets());
				}catch(NoSuchElementException e){
					unloadable.add(finalCurrent.getScheme());
					}
			});
		}
		service.shutdown();
		System.out.println("Shutting down...");
		service.awaitTermination(5, TimeUnit.MINUTES);
		System.out.println("Finished run!  Checked " + checked.size() + " URLS, found " + queue.size() + " more!");
		unloadable.forEach(p-> System.out.println("No protocol handler for " + p));
		unparsable.addAll(Parser.SPI.getLoggedMisses());
		unparsable.forEach(m-> System.out.println("No parser for " + m));
		System.out.println("============================");
		loadable.stream().map(URI::toASCIIString).forEach(System.out::println);
	}
}
