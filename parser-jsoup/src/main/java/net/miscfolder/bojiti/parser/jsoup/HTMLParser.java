package net.miscfolder.bojiti.parser.jsoup;

import java.io.CharArrayReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.worker.SPI;
import org.jsoup.nodes.*;

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
	public Set<URI> parse(URL url, CharSequence chars){
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

	Set<URI> parseWithFlattenedNodeChain(URL original, URL base, Document document){
		Set<URI> uris = new HashSet<>();
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
						uris.addAll(Cache.CSS.get().parse(base, ((DataNode)node).getWholeData()));
				}else if("script".equalsIgnoreCase(parent.nodeName())){
					if(parent.hasAttr("type")){
						Parser scriptParser = SPI.Parsers.getFirst(parent.attr("type"));
						if(scriptParser != null)
							uris.addAll(scriptParser.parse(base, ((DataNode)node).getWholeData()));
					}else if(Cache.JS.get() != null){
						uris.addAll(Cache.JS.get().parse(base, ((DataNode)node).getWholeData()));
					}
				}else{
					announce(l->l.onParserError(original,
							new MysteryDataNodeException((DataNode)node, parent)));
				}
			}else if(node.nodeName().equalsIgnoreCase("svg")){
				if(Cache.SVG.get() != null)
					uris.addAll(Cache.SVG.get().parse(base, node.outerHtml()));
			}else{
				// Mine the attributes
				for(Attribute attribute : node.attributes()){
					String key = attribute.getKey();
					String value = attribute.getValue();
					if("href".equalsIgnoreCase(key) ||
							"src".equalsIgnoreCase(key)){
						uris.add(resolve(base, value));
					}else if(key.startsWith("on")){
						if(Cache.JS.get() != null){
							uris.addAll(Cache.JS.get().parse(base, value));
						}
					}
				}
			}
		});
		if(Cache.TEXT.get() != null){
			uris.addAll(Cache.TEXT.get().parse(base, comments.toString()));
			uris.addAll(Cache.TEXT.get().parse(base, content.toString()));
		}
		uris.remove(null);

		return uris;
	}

	private static Stream<Node> flatten(Node parent){
		if(parent.childNodeSize() == 0 || parent.nodeName().equalsIgnoreCase("svg"))
			return Stream.of(parent);
		return Stream.concat(Stream.of(parent), parent.childNodes().stream().flatMap(HTMLParser::flatten));
	}

	URI resolve(URL base, String target){
		// Filter bracket selectors
		if((target.startsWith("{[") && target.endsWith("]}")) ||
				(target.startsWith("[[") && target.endsWith("]]")))
			return null;
		try{
			if(target.startsWith("//"))
				return new URI(base.getProtocol(), target, null);
			return new URL(base, target).toURI();
		}catch(MalformedURLException | URISyntaxException e){
			announce(l->l.onParserError(base, new ResolutionException(base, target, e)));
			return null;
		}
	}
}
