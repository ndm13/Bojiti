package net.miscfolder.bojiti.parser.regex;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.ParserException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MimeTypes("text/css")
public class CssParser extends RegexBasedParser{
	// Ported from v6
	private static final Pattern PATTERN =
			Pattern.compile("(url|@import)\\(([\'\"]?[^\'\") \n]*)", Pattern.CASE_INSENSITIVE);

	@Override
	public Set<URI> parse(URL url, CharSequence sequence, Consumer<ParserException> callback, IntConsumer count){
		Matcher matcher = PATTERN.matcher(sequence);
		Set<URI> matches = new HashSet<>();

		while(matcher.find()){
			try{
				matches.add(new URI(finesse(url, matcher.group(2), true)));
				count.accept(matches.size());
			}catch(URISyntaxException e){
				callback.accept(new RegexParserException(e, sequence, matcher, 2));
			}
		}
		return matches;
	}
}
