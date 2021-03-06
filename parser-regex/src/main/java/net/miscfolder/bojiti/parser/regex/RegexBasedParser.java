package net.miscfolder.bojiti.parser.regex;

import net.miscfolder.bojiti.parser.Parser;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RegexBasedParser implements Parser{
	private static final Pattern HAS_PROTOCOL = Pattern.compile("^[\\w\\d]{2,}:(\\S*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern LOCAL = Pattern.compile("^(\\.{0,2}/)+[\\p{L}\\d-_~%+]*(.*)$", Pattern.CASE_INSENSITIVE);

	private static final String badStartChars = "'\":";

	public static String finesse(URL parent, String input, boolean probablyRelative){
		while(badStartChars.indexOf(input.charAt(0)) > -1)
			input = input.substring(1);
		while(input.endsWith("."))  // TextParser sometimes includes ending periods
			input = input.substring(0,input.length()-1);

		if(probablyRelative){
			switch(input.charAt(0)){
				case '.':
				case '/':
				case '#':
					break;
				default:
					if(!HAS_PROTOCOL.matcher(input).matches())
						input = "./" + input;
			}
		}

		if(LOCAL.matcher(input).matches()){
			try{
				while(input.startsWith("./")) input = input.substring(2);
				return new URL(parent, input).toExternalForm();
			}catch(MalformedURLException ignore){}
		}

		URI uri = null;
		try{
			uri = new URI(input);
		}catch(URISyntaxException ignore){}

		if(uri == null || uri.getScheme() == null){
			if(input.startsWith("//")){
				input = parent.getProtocol() + ':' + input;
			}else if(!HAS_PROTOCOL.matcher(input).matches()){
				if(input.indexOf('@') > -1 &&       // Has @
						input.indexOf('/') == -1 && // No path
						input.indexOf(':') == -1){  // No port
					input = "mailto:" + input;
				}else{
					input = getProtocolWithSeperator(parent) + input;
				}
			}
		}
		return input;
	}

	private static String getProtocolWithSeperator(URL parent){
		String proto = parent.getProtocol();
		String separator = Pattern.compile(":[/]*")
				.matcher(parent.toExternalForm())
				.region(proto.length(), parent.toExternalForm().length())
				.results()
				.min(Comparator.comparingInt(MatchResult::start))
				.map(MatchResult::group)
				.orElse("://"); // Shouldn't be reached if URL is valid
		return proto + separator;
	}

	static CharSequence multimatch(CharSequence input, Map<Pattern,String> replacementMap){
		CharSequence cache = input;
		for(Map.Entry<Pattern,String> replacement : replacementMap.entrySet()){
			StringBuilder sb = new StringBuilder(cache.length());
			Matcher matcher = replacement.getKey().matcher(cache);
			while(matcher.find())
				matcher.appendReplacement(sb, replacement.getValue());
			matcher.appendTail(sb);
			cache = sb;
		}
		return cache;
	}
}
