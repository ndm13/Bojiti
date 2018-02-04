package net.miscfolder.bojiti.parser.regex;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.miscfolder.bojiti.parser.MimeTypes;

@MimeTypes("text/css")
public class CssParser extends RegexBasedParser{
	// Ported from v6
	private static final Pattern PATTERN =
			Pattern.compile("(url|@import)\\(([\'\"]?[^\'\") \n]*)", Pattern.CASE_INSENSITIVE);

	@Override
	public Set<URL> parse(URL url, CharBuffer charBuffer){
		return parse(url,(CharSequence) charBuffer);
	}

	@Override
	public Set<URL> parse(URL url, String string){
		return parse(url, (CharSequence) string);
	}

	private Set<URL> parse(URL url, CharSequence sequence){
		Matcher matcher = PATTERN.matcher(sequence);
		Set<URL> matches = new HashSet<>();

		while(matcher.find()){
			try{
				matches.add(new URL(finesse(url, matcher.group(2))));
			}catch(MalformedURLException e){
				// TODO fix
				System.err.println("ERROR: Couldn't create URL:\n\t" +
						url.toExternalForm() + " -> " + matcher.group(2) + "\n\t" +
						e.getLocalizedMessage() + "\n\tContext:\t\"" +
						sequence.subSequence(
								matcher.start() - 20, matcher.end() + 20) + '"');

			}
		}
		return matches;
	}
}
