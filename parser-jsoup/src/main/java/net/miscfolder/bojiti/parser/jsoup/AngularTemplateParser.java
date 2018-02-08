package net.miscfolder.bojiti.parser.jsoup;

import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.miscfolder.bojiti.parser.MimeTypes;

// Angular templates are essentially chunks of HTML with ng variables
// thrown in.  We can treat it as HTML and throw out any URLs that
// use ng variables, since they don't make sense without context.
@MimeTypes("text/ng-template")
public class AngularTemplateParser extends HTMLParser{
	private static final Pattern DOUBLE_BRACES = Pattern.compile("\\{\\{.*}}");

	@Override
	public Set<URL> parse(URL url, CharSequence chars){
		return super.parse(url, chars)
				.stream()
				.filter(found->!DOUBLE_BRACES.matcher(found.toExternalForm()).matches())
				.collect(Collectors.toSet());
	}
}
