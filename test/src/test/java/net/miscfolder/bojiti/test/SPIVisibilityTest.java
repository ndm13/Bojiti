package net.miscfolder.bojiti.test;

import java.util.Collection;

import net.miscfolder.bojiti.SPI;

public class SPIVisibilityTest{
	public static void main(String[] args){
		System.out.println("Loaded Downloaders:");
		SPI.Downloaders.getCacheSnapshot().values().stream()
				.flatMap(Collection::stream)
				.distinct()
				.forEach(d->System.out.println('\t' + d.getClass().getName()));
		System.out.println();
		System.out.println("Loaded Parsers:");
		SPI.Parsers.getCacheSnapshot().values().stream()
				.flatMap(Collection::stream)
				.distinct()
				.forEach(d->System.out.println('\t' + d.getClass().getName()));
		System.out.println("\n====================\n");
		SPI.Downloaders.getCacheSnapshot().forEach((k,v)->{
			System.out.println("Downloaders for '" + k + "':");
			v.forEach(d->System.out.println('\t' + d.getClass().getName()));
			System.out.println();
		});
		System.out.println("====================\n");
		SPI.Parsers.getCacheSnapshot().forEach((k,v)->{
			System.out.println("Parsers for '" + k + "':");
			v.forEach(d->System.out.println('\t' + d.getClass().getName()));
			System.out.println();
		});
	}
}
