package net.miscfolder.bojiti.parser.jsoup;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.ParserException;

import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Angular templates are essentially chunks of HTML with ng variables
// thrown in.  We can treat it as HTML and throw out any URLs that
// use ng variables, since they don't make sense without context.
@MimeTypes("text/ng-template")
public class AngularTemplateParser extends HTMLParser{
	private static final Pattern DOUBLE_BRACES = Pattern.compile("\\{\\{.*}}");

	@Override
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback){
		return super.parse(url, chars, callback)
				.stream()
				.filter(found->!DOUBLE_BRACES.matcher(found.toASCIIString()).matches())
				.collect(Collectors.toSet());
	}
}
