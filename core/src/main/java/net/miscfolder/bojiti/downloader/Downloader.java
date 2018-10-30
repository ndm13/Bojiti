package net.miscfolder.bojiti.downloader;

import net.miscfolder.bojiti.support.SPI;

import java.io.IOException;
import java.net.URL;

public interface Downloader{
	SPI<Downloader,Protocols> SPI =
			new SPI<>(Downloader.class, Protocols.class, Protocols::value);

	Response download(URL url) throws IOException, RedirectionException;
	
}
