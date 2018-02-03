package net.miscfolder.bojiti.parser;

import java.net.URL;
import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Parser{
	private final Set<Listener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public abstract Set<URL> parse(URL url, CharBuffer charBuffer);
	public abstract Set<URL> parse(URL url, String string);

	public void addUpdateListener(Listener listener){
		listeners.add(listener);
	}
	public void removeUpdateListener(Listener listener){
		listeners.remove(listener);
	}
	protected void onUpdate(URL url, int found, double progress){
		listeners.forEach(l->l.onParserUpdate(url, found, progress));
	}

	public interface Listener{
		void onParserUpdate(URL url, int found, double progress);
	}
}
