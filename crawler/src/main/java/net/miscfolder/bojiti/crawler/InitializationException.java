package net.miscfolder.bojiti.crawler;

public class InitializationException extends Exception{
	public InitializationException(String message, Exception parent){
		super(message, parent);
	}
	public InitializationException(String message){
		super(message);
	}
}
