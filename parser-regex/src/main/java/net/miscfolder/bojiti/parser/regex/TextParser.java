package net.miscfolder.bojiti.parser.regex;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.miscfolder.bojiti.parser.MimeTypes;

@MimeTypes("text/plain")
public class TextParser extends RegexBasedParser{
	private static final Pattern
			OBFUSCATED_AT = Pattern.compile("[\\s\\[({/:_-]*(at|@)[]/)}\\s]*", Pattern.CASE_INSENSITIVE),
			OBFUSCATED_DOT = Pattern.compile("[\\s\\[({/:_-]*(dot|\\.)[]/)}\\s]*", Pattern.CASE_INSENSITIVE),
			NOSPAM = Pattern.compile("[-._]?\\s*[\\[({:_-]*no[\\s-._]*spam[])}\\s]*", Pattern.CASE_INSENSITIVE),
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
				announce(l->l.onParserError(url,
						new RegexParserException(e, deobfuscated, matcher)));
			}
		}

		return matches;
	}
}
