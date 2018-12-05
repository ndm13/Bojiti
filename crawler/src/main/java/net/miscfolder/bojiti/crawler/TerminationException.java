package net.miscfolder.bojiti.crawler;

public class TerminationException extends Exception{
	public TerminationException(String message, Exception parent){
		super(message, parent);
	}
	public TerminationException(String message){
		super(message);
	}
}
