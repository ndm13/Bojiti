package net.miscfolder.bojiti.parser.regex;

import java.net.MalformedURLException;
import java.util.regex.Matcher;

import net.miscfolder.bojiti.parser.ParserException;

public class RegexParserException extends ParserException{
	private final String target, context;

	RegexParserException(MalformedURLException cause, CharSequence source, Matcher matcher, int index){
		this(cause, matcher.group(index), matcherContext(source, matcher).toString());
	}

	RegexParserException(MalformedURLException cause, CharSequence source, Matcher matcher){
		this(cause, matcher.group(), matcherContext(source, matcher).toString());
	}

	private RegexParserException(MalformedURLException cause, String target, String context){
		super("Parsing failed for target: '" + target + "' context: '" + context + '\'', cause);
		this.target = target;
		this.context = context;
	}

	public String getContext(){
		return context;
	}

	public String getTarget(){
		return target;
	}

	private static CharSequence matcherContext(CharSequence sequence, Matcher matcher){
		int start, end;
		start = Math.max(matcher.start() - 10, 0);
		end = Math.min(matcher.end() + 10, sequence.length());
		return sequence.subSequence(start, end);
	}
}
