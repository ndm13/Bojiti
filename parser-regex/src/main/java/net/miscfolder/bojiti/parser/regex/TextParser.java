package net.miscfolder.bojiti.parser.regex;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;

@MimeTypes("text/plain")
public class TextParser extends Parser{
	protected static final Pattern
			OBFUSCATED_AT = Pattern.compile("[\\s\\[({/:_-]+\\s*(at|@)\\s*[\\]/)}\\s]+", Pattern.CASE_INSENSITIVE),
			OBFUSCATED_DOT = Pattern.compile("[\\s\\[({/:_-]+\\s*(dot|\\.)\\s*[\\]/)}\\s]+", Pattern.CASE_INSENSITIVE),
			NOSPAM = Pattern.compile("[-._]?\\s*[\\[({:_-]*\\s*no[\\s-._]*spam\\s*[\\])}]*\\s*", Pattern.CASE_INSENSITIVE),
			TEXT_URL_FINDER = Pattern.compile("([a-z0-9]+:)?" +
					"(//)?" +
					"([\\p{L}\\d]+(:[\\p{L}\\d-_~]+)?@)?" +
					"(([\\d\\p{L}-_~]+\\.)+\\p{L}{2,63}+|(\\d{1,3}\\.){3}\\d{1,3}|localhost|(\\[[\\da-f:.]{3,45}]))" +
					"(:\\d+)?" +
					"(/[\\d\\p{L}-_~%+.:()]*)*" +
					"(\\.[\\d\\p{L}-_%+.:()]+)?" +
					"(\\?[\\d\\p{L}-_%~=&+.:*!()]+)?" +
					"(#[\\d\\p{L}-_~%+.:*!()]*)?", Pattern.CASE_INSENSITIVE);

	private static final Pattern HAS_PROTOCOL = Pattern.compile("^[\\w\\d]{2,}:(\\S*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern LOCAL = Pattern.compile("^(\\.{0,2}/)+[\\p{L}\\d-_~%+]+(.*)$", Pattern.CASE_INSENSITIVE);


	@Override
	public Set<URL> parse(URL url, String string){
		return parse(url, (CharSequence) string);
	}

	@Override
	public Set<URL> parse(URL url, CharBuffer charBuffer){
		return parse(url, (CharSequence) charBuffer);
	}

	public Set<URL> parse(URL url, CharSequence chars){
		Map<Pattern,String> replacementMap = new HashMap<>();
		replacementMap.put(NOSPAM,"");
		replacementMap.put(OBFUSCATED_AT,"@");
		replacementMap.put(OBFUSCATED_DOT,".");
		String deobfuscated = multimatch(chars, replacementMap).toString();
		Set<URL> matches = new HashSet<>();
		Matcher matcher = TEXT_URL_FINDER.matcher(deobfuscated);

		while(matcher.find()){
			try{
				matches.add(new URL(finesse(url, matcher.group())));
			}catch(MalformedURLException e){
				// TODO fix
				System.err.println("ERROR: Couldn't create URL:\n\t" +
						url.toExternalForm() + " -> " + matcher.group() + "\n\t" +
						e.getLocalizedMessage() + "\n\tContext:\t\"" +
						deobfuscated.substring(
								matcher.start() - 20, matcher.end() + 20) + '"');

			}
		}

		return matches;
	}

	private static String finesse(URL parent, String input){
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

	private static CharSequence multimatch(CharSequence input, Map<Pattern,String> replacementMap){
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
