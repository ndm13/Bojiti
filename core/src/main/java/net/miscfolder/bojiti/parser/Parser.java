package net.miscfolder.bojiti.parser;

import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.miscfolder.bojiti.internal.Announcer;

public abstract class Parser implements Announcer<Parser.Listener>{
	private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

	public abstract Set<URI> parse(URL url, CharSequence chars);

	@Override
	public Set<Listener> listeners(){
		return listeners;
	}

	public interface Listener{
		void onParserUpdate(URL url, int found, double progress);
		void onParserError(URL url, ParserException exception);
	}
}
