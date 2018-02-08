package net.miscfolder.bojiti.test.backend;

import java.net.URL;
import java.util.Iterator;

import net.miscfolder.bojiti.worker.Worker;

public interface Backend extends Iterator<URL>, Worker.Listener{
	void add(URL... urls);
}
