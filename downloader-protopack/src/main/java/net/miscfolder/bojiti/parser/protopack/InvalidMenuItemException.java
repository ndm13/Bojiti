package net.miscfolder.bojiti.parser.protopack;

import net.miscfolder.bojiti.parser.ParserException;

public class InvalidMenuItemException extends ParserException{
	private final String[] parts;

	InvalidMenuItemException(String[] parts, String message, Exception exception){
		super(message, exception);
		this.parts = parts;
	}

	public String[] getParts(){
		return parts;
	}
}
