package net.miscfolder.bojiti.parser;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.miscfolder.bojiti.internal.Announcer;

public abstract class Parser implements Announcer<Parser.Listener>{
	private final Set<Listener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public abstract Set<URL> parse(URL url, CharSequence chars);

	@Override
	public Set<Listener> listeners(){
		return listeners;
	}

	public interface Listener{
		void onParserUpdate(URL url, int found, double progress);
		void onParserError(URL url, ParserException exception);
	}
}
