package net.miscfolder.bojiti.parser.regex;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.ParserException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MimeTypes("text/plain")
public class TextParser extends RegexBasedParser{
	private static final System.Logger LOGGER = System.getLogger(TextParser.class.getName());

	private static final Pattern
			OBFUSCATED_AT = Pattern.compile("([\\s\\[({/:_-]+(at|@)[]/)}\\s]+|\\s@|@\\s)", Pattern.CASE_INSENSITIVE),
			OBFUSCATED_DOT = Pattern.compile("[\\[({/:_\\-\\s]*([dD][oO][tT]|\\.)[]/)}\\s]*(?![A-Z][a-z])"),
			OBFUSCATED_SLASH = Pattern.compile("([\\s\\[({/_-]+(slash|/)[]/)}\\s]+|\\s/|/\\s)", Pattern.CASE_INSENSITIVE),
			OBFUSCATED_DASH = Pattern.compile("([\\s\\[({/_-]+(dash|-)[]/)}\\s]+|\\s/|/\\s)", Pattern.CASE_INSENSITIVE),
			NOSPAM = Pattern.compile("[-._]?\\s*[\\[({:_-]*no[\\s-._]*spam[])}\\s]*", Pattern.CASE_INSENSITIVE),
			WIDE_SPACE = Pattern.compile("\\s+"),
			TEXT_URL_FINDER = Pattern.compile("([a-z0-9]+:)?" +
					"(//)?" +
					"([\\p{L}\\d]+(:[\\p{L}\\d-_~]+)?@)?" +
					"(([\\d\\p{L}-_~]+\\.)+\\p{L}{2,63}+|(\\d{1,3}\\.){3}\\d{1,3}|localhost|(\\[[\\da-f:.]{3,45}]))" +
					"(:\\d+)?" +
					"(/[\\d\\p{L}-_~%+.:()]*)*" +
					"(\\.[\\d\\p{L}-_%+.:()]+)?" +
					"(\\?[\\d\\p{L}-_%~=&+.:*!()]+)?" +
					"(#[\\d\\p{L}-_~%+.:*!()]*)?", Pattern.CASE_INSENSITIVE);

	@Override
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback, IntConsumer count){
		Map<Pattern,String> replacementMap = new HashMap<>();
		replacementMap.put(WIDE_SPACE, " ");
		replacementMap.put(NOSPAM,"");
		replacementMap.put(OBFUSCATED_AT,"@");
		replacementMap.put(OBFUSCATED_DOT,".");
		replacementMap.put(OBFUSCATED_DASH,"-");
		replacementMap.put(OBFUSCATED_SLASH,"/");
		String deobfuscated = multimatch(chars, replacementMap).toString();
		chars = null;   // GC
		Set<URI> matches = new HashSet<>();
		Matcher matcher = TEXT_URL_FINDER.matcher(deobfuscated);

		while(matcher.find()){
			URI absolute = null;
			try{
				absolute = new URI(finesse(url, matcher.group(), false));
				LOGGER.log(System.Logger.Level.INFO, "PARSED:\n\t" +
						RegexParserException.matcherContext(deobfuscated, matcher) +
						"\n\t" + absolute.toASCIIString() + "\n\tvia " + url.toExternalForm());
				matches.add(absolute);
				count.accept(matches.size());
			}catch(URISyntaxException e){
				callback.accept(new RegexParserException(e, deobfuscated, matcher));
			}
			try{
				URI relative = new URI(finesse(url, matcher.group(), true));
				// We don't want to add a relative version if it has a user info section;
				// those are reserved for things like mailto and make no sense otherwise.
				// The double-check was intended for ambiguous name.ext anyway.
				if(absolute == null || (absolute.getRawUserInfo() == null &&
						(absolute.getSchemeSpecificPart() == null || !absolute.getSchemeSpecificPart().contains("@")))){
					LOGGER.log(System.Logger.Level.INFO, "PARSED:\n\t" +
							RegexParserException.matcherContext(deobfuscated, matcher) +
							"\n\t" + relative.toASCIIString() + "\n\tvia " + url.toExternalForm());
					matches.add(relative);
					count.accept(matches.size());
				}
			}catch(URISyntaxException e){
				callback.accept(new RegexParserException(e, deobfuscated, matcher));
			}
		}

		return matches;
	}
}
