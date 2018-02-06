package net.miscfolder.bojiti.parser.jsoup;

import java.net.MalformedURLException;
import java.net.URL;

import net.miscfolder.bojiti.parser.ParserException;

class ResolutionException extends ParserException{
	ResolutionException(URL base, String target,
			MalformedURLException exception){
		super("Couldn't resolve secondary domain: " +
				base.toExternalForm() + " -> " + target, exception);
	}
}
