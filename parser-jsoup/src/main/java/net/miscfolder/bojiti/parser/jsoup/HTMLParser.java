package net.miscfolder.bojiti.parser.jsoup;

import java.io.CharArrayReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import net.miscfolder.bojiti.SPI;
import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

@MimeTypes({"text/html","text/xhtml","application/xhtml","application/xml+html"})
public class HTMLParser extends Parser{
	@Override
	public Set<URL> parse(URL base, CharSequence chars){
		// TODO more parsers
		Parser css = SPI.Parsers.getFirst("text/css"),
				js = SPI.Parsers.getFirst("text/javascript"),
				svg = SPI.Parsers.getFirst("image/svg"),
				text = SPI.Parsers.getFirst("text/plain");

		if(js == null) js = SPI.Parsers.getFirst("application/javascript");
		if(svg == null) SPI.Parsers.getFirst("text/svg");

		// We've got a 99% chance of this being a CharBuffer.
		// We can take advantage of this using CharArrayReader.
		Document document = (chars instanceof CharBuffer ?
				            usingReader((CharBuffer) chars, base) :
							usingString(chars.toString(), base));

		try{
			// If the document sets a base URL, use that
			if(document.baseUri() != null)
				base = new URL(document.baseUri());
		}catch(MalformedURLException ignore){}

		return onceOverNodeParser(base, document, css, js, svg, text);
	}

	private Document usingString(String string, URL url){
		return org.jsoup.parser.Parser.parse(string, url.toExternalForm());
	}

	private Document usingReader(CharBuffer buffer, URL url){
		return org.jsoup.parser.Parser.htmlParser()
				.parseInput(new CharArrayReader(buffer.array()), url.toExternalForm());
	}

	Set<URL> onceOverNodeParser(URL base, Document document,
			Parser css, Parser js, Parser svg, Parser text){
		Set<URL> scraped = new HashSet<>();
		// This approach is pretty heavy.  Jsoup does all the validation we're
		// looking for, but the NodeVisitor framework is a little heavy, esp.
		// with multiple iterations on massive (hundreds of MB+) documents.
		// We should iterate all the nodes once and accumulate like normal, but
		// also pick up Comments.
		StringBuilder
				comments = new StringBuilder(),
				content = new StringBuilder();
		flatten(document).forEachOrdered(node->{
			if(node instanceof Comment){
				comments.append(node.attr(node.nodeName())).append('\n');
			}else if(node instanceof TextNode){
				content.append(((TextNode)node).text()).append(' ');
			}else if(node instanceof DataNode){
				// This is where we parse things like <style> and <script>
				// ("like": in a perfect world, that is.  Current Jsoup only
				// assigns DataNode to these types.
				Node parent = node.parentNode();
				if("style".equalsIgnoreCase(parent.nodeName())){
					if(css != null)
						scraped.addAll(css.parse(base, ((DataNode)node).getWholeData()));
				}else if("script".equalsIgnoreCase(parent.nodeName())){
					if(parent.hasAttr("type")){
						Parser scriptParser = SPI.Parsers.getFirst(parent.attr("type"));
						if(scriptParser != null)
							scraped.addAll(scriptParser.parse(base, ((DataNode)node).getWholeData()));
					}else if(js != null){
						scraped.addAll(js.parse(base, ((DataNode)node).getWholeData()));
					}
				}else{
					announce(l->l.onParserError(base,
							new MysteryDataNodeException((DataNode)node, parent)));
				}
			}else if(node.nodeName().equalsIgnoreCase("svg")){
				if(svg != null)
					scraped.addAll(svg.parse(base, node.outerHtml()));
			}else{
				// Mine the attributes
				for(Attribute attribute : node.attributes()){
					String key = attribute.getKey();
					String value = attribute.getValue();
					if("href".equalsIgnoreCase(key) ||
							"src".equalsIgnoreCase(key)){
						scraped.add(resolve(base, value));
					}else if(key.startsWith("on")){
						if(js != null){
							scraped.addAll(js.parse(base, value));
						}
					}
				}
			}
		});
		if(text != null){
			scraped.addAll(text.parse(base, comments.toString()));
			scraped.addAll(text.parse(base, content.toString()));
		}
		scraped.remove(null);

		return scraped;
	}

	Set<URL> traditionalDOMParser(URL base, Document document,
			Parser css, Parser js, Parser svg, Parser text){
		Set<URL> scraped = new HashSet<>();
		announce(l->l.onParserUpdate(base, 0, (double)0));
		Elements elements = document.getAllElements();
		int size = elements.size();
		AtomicInteger found = new AtomicInteger();
		int processed = 0;
		for(Element element : elements){
			double progress = ((double)++processed / (double)size) * 0.9;
			for(Attribute attribute : element.attributes()){
				if(attribute.getKey().equalsIgnoreCase("href")){
					URL url = resolve(base, attribute.getValue());
					if(url != null && scraped.add(url)){
						announce(l->l.onParserUpdate(base, found.incrementAndGet(), progress));
					}
				}
				if(attribute.getKey().equalsIgnoreCase("src")){
					URL url = resolve(base, attribute.getValue());
					if(url != null && scraped.add(url)){
						announce(l->l.onParserUpdate(base, found.incrementAndGet(), progress));
					}
				}
				if(css != null && attribute.getKey().equalsIgnoreCase("style")){
					updateExternal(css.parse(base, attribute.getValue()), scraped, base, found, progress);
				}
				if(js != null && attribute.getKey().startsWith("on")){
					updateExternal(js.parse(base, attribute.getValue()), scraped, base, found, progress);
				}
			}
			if(css != null && element.tagName().equalsIgnoreCase("style")){
				updateExternal(css.parse(base, element.html()), scraped, base, found, progress);
			}
			if("script".equalsIgnoreCase(element.tagName()) && element.hasText()){
				if(element.hasAttr("type")){
					Parser scriptParser = SPI.Parsers.getFirst(element.attr("type"));
					if(scriptParser != null){
						updateExternal(scriptParser.parse(base, element.html()),
								scraped, base, found, progress);
					}
				}else if(js != null){
					// Probably vanilla JS
					updateExternal(js.parse(base, element.html()), scraped, base, found, progress);
				}
			}
			if(svg != null && element.tagName().equalsIgnoreCase("svg")){
				updateExternal(svg.parse(base, element.html()), scraped, base, found, progress);
			}
		}
		if(text != null){
			updateExternal(text.parse(base, document.text()), scraped, base, found, 0.9);
		}
		announce(l->l.onParserUpdate(base, found.get(), 1));

		return scraped;
	}

	private static Stream<Node> flatten(Node parent){
		if(parent.childNodeSize() == 0 || parent.nodeName().equalsIgnoreCase("svg"))
			return Stream.of(parent);
		return Stream.concat(Stream.of(parent), parent.childNodes().stream().flatMap(HTMLParser::flatten));
	}

	private void updateExternal(Set<URL> external, Set<URL> scraped, URL base, AtomicInteger found, double progress){
		int before = scraped.size();
		scraped.addAll(external);
		announce(l->l.onParserUpdate(base, found.addAndGet(scraped.size() - before), progress));
	}


	URL resolve(URL base, String target){
		// Filter bracket selectors
		if((target.startsWith("{[") && target.endsWith("]}")) ||
				(target.startsWith("[[") && target.endsWith("]]")))
			return null;
		try{
			return new URL(base, target);
		}catch(MalformedURLException e){
			announce(l->l.onParserError(base, new ResolutionException(base, target, e)));
			return null;
		}
	}
}
