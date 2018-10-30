package net.miscfolder.bojiti.test;

import java.util.Collection;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.parser.Parser;

public class SPIVisibilityTest{
	public static void main(String[] args){
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
			System.out.println("SPI for '" + k + "':");
			v.forEach(d->System.out.println('\t' + d.getClass().getName()));
			System.out.println();
		});
		System.out.println("====================\n");
		Parser.SPI.getCacheSnapshot().forEach((k,v)->{
			System.out.println("SPI for '" + k + "':");
			v.forEach(d->System.out.println('\t' + d.getClass().getName()));
			System.out.println();
		});
	}
}
