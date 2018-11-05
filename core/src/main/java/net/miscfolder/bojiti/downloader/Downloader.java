package net.miscfolder.bojiti.downloader;

import net.miscfolder.bojiti.support.SPI;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public interface Downloader{
	SPI<Downloader,Protocols> SPI =
			new SPI<>(Downloader.class, Protocols.class, Protocols::value);

	Response download(URL url, Consumer<Response.Progress> callback) throws IOException, RedirectionException;
	
}
