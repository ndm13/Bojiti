package net.miscfolder.bojiti.test;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.parser.Parser;

import java.util.Collection;
import java.util.ServiceLoader;

public class SPIVisibilityTest{
	public static void main(String[] args){
		System.out.println("ServiceLoader finds:");
		ServiceLoader.load(Downloader.class).forEach(d->System.out.println('\t' + d.getClass().getName()));
		System.out.println();
		System.out.println("ServiceLoader finds:");
		ServiceLoader.load(Parser.class).forEach(d->System.out.println('\t' + d.getClass().getName()));
		System.out.println();

		System.out.println("Loaded SPI:");
		Downloader.SPI.getCacheSnapshot().values().stream()
				.flatMap(Collection::stream)
				.distinct()
				.forEach(d->System.out.println('\t' + d.getClass().getName()));
		System.out.println();
		System.out.println("Loaded SPI:");
		Parser.SPI.getCacheSnapshot().values().stream()
				.flatMap(Collection::stream)
				.distinct()
				.forEach(d->System.out.println('\t' + d.getClass().getName()));
		System.out.println("\n====================\n");
		Downloader.SPI.getCacheSnapshot().forEach((k,v)->{
			System.out.println("Downloader for '" + k + "':");
			v.forEach(d->System.out.println('\t' + d.getClass().getName()));
			System.out.println();
		});
		System.out.println("====================\n");
		Parser.SPI.getCacheSnapshot().forEach((k,v)->{
			System.out.println("Parser for '" + k + "':");
			v.forEach(d->System.out.println('\t' + d.getClass().getName()));
			System.out.println();
		});
	}
}
