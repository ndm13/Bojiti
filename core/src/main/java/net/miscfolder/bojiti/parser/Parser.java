package net.miscfolder.bojiti.parser;

import net.miscfolder.bojiti.support.SPI;

import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public interface Parser{
	SPI<Parser,MimeTypes> SPI = new SPI<>(Parser.class, MimeTypes.class, MimeTypes::value);

	Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> errorCallback, IntConsumer countCallback);

	default Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback){
		return parse(url, chars, callback, i->{});
	}

	default Set<URI> parse(URL url, CharSequence chars){
		return parse(url, chars, x->{});
	}
}
