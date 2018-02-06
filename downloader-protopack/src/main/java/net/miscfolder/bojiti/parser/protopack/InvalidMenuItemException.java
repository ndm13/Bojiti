package net.miscfolder.bojiti.parser.protopack;

import java.util.Arrays;

import net.miscfolder.bojiti.parser.ParserException;

public class InvalidMenuItemException extends ParserException{
	private final String[] parts;

	InvalidMenuItemException(String[] parts, String message, Exception exception){
		super(message + "\n\tParts: " + Arrays.toString(parts), exception);
		this.parts = parts;
	}

	public String[] getParts(){
		return parts;
	}
}
