package net.miscfolder.bojiti.parser;

import net.miscfolder.bojiti.support.SPI;

import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.function.Consumer;

public interface Parser{
	SPI<Parser,MimeTypes> SPI = new SPI<>(Parser.class, MimeTypes.class, MimeTypes::value);

	Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback);
}
