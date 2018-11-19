package net.miscfolder.bojiti.downloader;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

public class RedirectionException extends Exception{
	private final Set<URI> targets;
	private final URL source;
	private final int status;
	private Response response;

	public RedirectionException(Response response, int status, Set<URI> redirectTargets){
		this.source = response.getURL();
		this.status = status;
		this.targets = Collections.unmodifiableSet(redirectTargets);
		this.response = response;
	}

	public URL getSource(){
		return source;
	}

	public int getStatus(){
		return status;
	}

	public Set<URI> getTargets(){
		return targets;
	}

	public Response getResponse(){
		return response;
	}

	@Override
	public String getMessage(){
		return "Redirect issued for " + source.toExternalForm() + " (targets: " + targets.size() + ")";
	}
}
