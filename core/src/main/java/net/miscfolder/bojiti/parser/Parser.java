package net.miscfolder.bojiti.parser;

import java.net.URI;
import java.net.URL;
import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.miscfolder.bojiti.support.Dispatcher;
import net.miscfolder.bojiti.support.SPI;

public abstract class Parser implements Dispatcher<Parser.Listener>{
	public static final SPI<Parser,MimeTypes> SPI =
			new SPI<>(Parser.class, MimeTypes.class, MimeTypes::value);
	private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

	public abstract Set<URI> parse(URL url, CharSequence chars);

	@Override
	public Set<Listener> getEventListeners(){
		return listeners;
	}

	public interface Listener extends EventListener{
		void onParserUpdate(URL url, int found, double progress);
		void onParserError(URL url, ParserException exception);
	}
}
