package net.miscfolder.bojiti.parser.jsoup;

import java.io.CharArrayReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.worker.SPI;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

@MimeTypes({"text/html","text/xhtml","application/xhtml","application/xml+html"})
public class HTMLParser extends Parser{
	private enum Cache{
		CSS(()->SPI.Parsers.getFirst("text/css")),
		SVG(()->Optional
				.ofNullable(SPI.Parsers.getFirst("image/svg"))
				.orElse(SPI.Parsers.getFirst("text/svg"))),
		JS(()->Optional
				.ofNullable(SPI.Parsers.getFirst("text/javascript"))
				.orElse(SPI.Parsers.getFirst("application/javascript"))),
		TEXT(()->SPI.Parsers.getFirst("text/plain"));

		private final Supplier<Parser> supplier;
		private boolean loaded = false;
		private Parser parser;

		Cache(Supplier<Parser> supplier){
			this.supplier = supplier;
		}
		public synchronized Parser get(){
			if(!loaded){
				parser = supplier.get();
				loaded = true;
			}
			return parser;
		}
	}

	@Override
	public Set<URL> parse(URL url, CharSequence chars){
		// We've got a 99% chance of this being a CharBuffer.
		// We can take advantage of this using CharArrayReader.
		Document document = (chars instanceof CharBuffer ?
				            loadWithReader((CharBuffer) chars, url) :
							loadWithString(chars.toString(), url));
		URL base = null;
		try{
			// If the document sets a base URL, use that
			if(document.baseUri() != null)
				base = new URL(document.baseUri());
		}catch(MalformedURLException ignore){
			base = url;
		}

		return parseWithFlattenedNodeChain(url, base, document);
	}

	private Document loadWithString(String string, URL url){
		return org.jsoup.parser.Parser.parse(string, url.toExternalForm());
	}

	private Document loadWithReader(CharBuffer buffer, URL url){
		return org.jsoup.parser.Parser.htmlParser()
				.parseInput(new CharArrayReader(buffer.array()), url.toExternalForm());
	}

	Set<URL> parseWithFlattenedNodeChain(URL original, URL base, Document document){
		Set<URL> scraped = new HashSet<>();
		StringBuilder
				comments = new StringBuilder(),
				content = new StringBuilder();
		flatten(document).forEachOrdered(node->{
			if(node instanceof Comment){
				comments.append(node.attr(node.nodeName())).append(' ');
			}else if(node instanceof TextNode){
				content.append(((TextNode)node).text()).append(' ');
			}else if(node instanceof DataNode){
				// This is where we parse things like <style> and <script>
				// ("like": in a perfect world, that is.  Current Jsoup only
				// assigns DataNode to these types.
				Node parent = node.parentNode();
				if("style".equalsIgnoreCase(parent.nodeName())){
					if(Cache.CSS.get() != null)
						scraped.addAll(Cache.CSS.get().parse(base, ((DataNode)node).getWholeData()));
				}else if("script".equalsIgnoreCase(parent.nodeName())){
					if(parent.hasAttr("type")){
						Parser scriptParser = SPI.Parsers.getFirst(parent.attr("type"));
						if(scriptParser != null)
							scraped.addAll(scriptParser.parse(base, ((DataNode)node).getWholeData()));
					}else if(Cache.JS.get() != null){
						scraped.addAll(Cache.JS.get().parse(base, ((DataNode)node).getWholeData()));
					}
				}else{
					announce(l->l.onParserError(original,
							new MysteryDataNodeException((DataNode)node, parent)));
				}
			}else if(node.nodeName().equalsIgnoreCase("svg")){
				if(Cache.SVG.get() != null)
					scraped.addAll(Cache.SVG.get().parse(base, node.outerHtml()));
			}else{
				// Mine the attributes
				for(Attribute attribute : node.attributes()){
					String key = attribute.getKey();
					String value = attribute.getValue();
					if("href".equalsIgnoreCase(key) ||
							"src".equalsIgnoreCase(key)){
						scraped.add(resolve(base, value));
					}else if(key.startsWith("on")){
						if(Cache.JS.get() != null){
							scraped.addAll(Cache.JS.get().parse(base, value));
						}
					}
				}
			}
		});
		if(Cache.TEXT.get() != null){
			scraped.addAll(Cache.TEXT.get().parse(base, comments.toString()));
			scraped.addAll(Cache.TEXT.get().parse(base, content.toString()));
		}
		scraped.remove(null);

		return scraped;
	}

	@Deprecated
	Set<URL> parseWithGetAllElements(URL original, URL base, Document document){
		Set<URL> scraped = new HashSet<>();
		announce(l->l.onParserUpdate(original, 0, 0));
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
						announce(l->l.onParserUpdate(original, found.incrementAndGet(), progress));
					}
				}
				if(attribute.getKey().equalsIgnoreCase("src")){
					URL url = resolve(base, attribute.getValue());
					if(url != null && scraped.add(url)){
						announce(l->l.onParserUpdate(original, found.incrementAndGet(), progress));
					}
				}
				if(Cache.CSS.get() != null && attribute.getKey().equalsIgnoreCase("style")){
					updateExternal(Cache.CSS.get().parse(base, attribute.getValue()), scraped, original, found, progress);
				}
				if(Cache.JS.get() != null && attribute.getKey().startsWith("on")){
					updateExternal(Cache.JS.get().parse(base, attribute.getValue()), scraped, original, found, progress);
				}
			}
			if(Cache.CSS.get() != null && element.tagName().equalsIgnoreCase("style")){
				updateExternal(Cache.CSS.get().parse(base, element.html()), scraped, original, found, progress);
			}
			if("script".equalsIgnoreCase(element.tagName()) && element.hasText()){
				if(element.hasAttr("type")){
					Parser scriptParser = SPI.Parsers.getFirst(element.attr("type"));
					if(scriptParser != null){
						updateExternal(scriptParser.parse(base, element.html()),
								scraped, original, found, progress);
					}
				}else if(Cache.JS.get() != null){
					// Probably vanilla JS
					updateExternal(Cache.JS.get().parse(base, element.html()), scraped, original, found, progress);
				}
			}
			if(Cache.SVG.get() != null && element.tagName().equalsIgnoreCase("svg")){
				updateExternal(Cache.SVG.get().parse(base, element.html()), scraped, original, found, progress);
			}
		}
		if(Cache.TEXT.get() != null){
			updateExternal(Cache.TEXT.get().parse(base, document.text()), scraped, original, found, 0.9);
		}
		announce(l->l.onParserUpdate(original, found.get(), 1));

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
