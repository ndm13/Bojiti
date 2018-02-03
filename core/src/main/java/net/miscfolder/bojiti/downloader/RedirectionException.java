package net.miscfolder.bojiti.downloader;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

public class RedirectionException extends Exception{
	private final Set<URL> targets;
	private final URL source;
	private final int status;

	public RedirectionException(URL source, int status, Set<URL> targets){
		this.source = source;
		this.status = status;
		this.targets = Collections.unmodifiableSet(targets);
	}

	public URL getSource(){
		return source;
	}

	public int getStatus(){
		return status;
	}

	public Set<URL> getTargets(){
		return targets;
	}

	@Override
	public String getMessage(){
		return "Redirect issued for " + source.toExternalForm() + " (targets: " + targets.size() + ")";
	}
}
