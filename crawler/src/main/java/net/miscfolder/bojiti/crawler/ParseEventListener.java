package net.miscfolder.bojiti.crawler;

import net.miscfolder.bojiti.parser.ParserException;

import java.net.URI;
import java.util.EventListener;

public interface ParseEventListener extends EventListener{
	void onBegin(URI uri);
	void onUpdate(URI uri, int count);
	void onException(URI uri, ParserException e);
	void onComplete(URI uri, int count);
}
