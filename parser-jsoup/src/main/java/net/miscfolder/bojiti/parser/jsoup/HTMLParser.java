package net.miscfolder.bojiti.parser.jsoup;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.bojiti.support.CharBufferReader;
import net.miscfolder.bojiti.parser.regex.RegexBasedParser;
import org.jsoup.nodes.*;

import java.io.CharArrayReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@MimeTypes({"text/html","text/xhtml","application/xhtml","application/xml+html"})
public class HTMLParser implements Parser{
	private enum Cache{
		CSS(()->Parser.SPI.getFirst("text/css")),
		SVG(()->Parser.SPI.getFirst("image/svg", "text/svg")),
		JS(()->Parser.SPI.getFirst("text/javascript", "application/javascript")),
		TEXT(()->Parser.SPI.getFirst("text/plain"));

		private final Supplier<Parser> supplier;
		private boolean loaded = false;
		private Parser parser;

		Cache(Supplier<Parser> supplier){
			this.supplier = supplier;
		}
		public synchronized Parser get(){
			if(!loaded){
				try{
					parser = supplier.get();
				}catch(NoSuchElementException e){
					parser = null;
				}
				loaded = true;
			}
			return parser;
		}
	}

	@Override
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback, IntConsumer count){
		// We've got a 99% chance of this being a CharBuffer.
		// We can take advantage of this using CharArrayReader.
		Document document = (chars instanceof CharBuffer ?
				            loadWithReader((CharBuffer) chars, url) :
							loadWithString(chars.toString(), url));
		chars = null;
		URL base = null;
		try{
			// If the document sets a base URL, use that
			if(document.baseUri() != null)
				base = new URL(document.baseUri());
		}catch(MalformedURLException ignore){
			base = url;
		}

		return parseWithFlattenedNodeChain(url, base, document, callback, count);
	}

	private Document loadWithString(String string, URL url){
		return org.jsoup.parser.Parser.parse(string, url.toExternalForm());
	}

	private Document loadWithReader(CharBuffer buffer, URL url){
		return org.jsoup.parser.Parser.htmlParser()
				.parseInput(buffer.hasArray() ? new CharArrayReader(buffer.array()) : new CharBufferReader(buffer), url.toExternalForm());
	}

	Set<URI> parseWithFlattenedNodeChain(URL original, URL base, Document document, Consumer<ParserException> callback,
	                                     IntConsumer count){
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
				// ("like": in a perfect world, that is; current Jsoup only
				// assigns DataNode to these types)
				Node parent = node.parentNode();
				if("style".equalsIgnoreCase(parent.nodeName())){
					if(Cache.CSS.get() != null)
						uris.addAll(Cache.CSS.get().parse(base, ((DataNode)node).getWholeData(), callback));
				}else if("script".equalsIgnoreCase(parent.nodeName())){
					if(parent.hasAttr("type")){
						try{
							Parser scriptParser = Parser.SPI.getFirst(parent.attr("type"));
							uris.addAll(scriptParser.parse(base, ((DataNode) node).getWholeData(), callback));
						}catch(NoSuchElementException ignore){ /* Best effort */ }
					}else if(Cache.JS.get() != null){
						uris.addAll(Cache.JS.get().parse(base, ((DataNode)node).getWholeData(), callback));
					}
				}else{
					callback.accept(new MysteryDataNodeException((DataNode)node, parent));
				}
			}else if(node.nodeName().equalsIgnoreCase("svg")){
				if(Cache.SVG.get() != null)
					uris.addAll(Cache.SVG.get().parse(base, node.outerHtml(), callback));
			}else{
				// Mine the attributes
				for(Attribute attribute : node.attributes()){
					String key = attribute.getKey();
					String value = attribute.getValue();
					if("href".equalsIgnoreCase(key) ||
							"src".equalsIgnoreCase(key)){
						try{
							uris.add(resolve(base, value));
						}catch(MalformedURLException | URISyntaxException e){
							callback.accept(new ResolutionException(base, value, e));
						}
					}else if(key.startsWith("on")){
						if(Cache.JS.get() != null){
							uris.addAll(Cache.JS.get().parse(base, value, callback));
						}
					}
				}
			}
			count.accept(uris.size());
		});
		if(Cache.TEXT.get() != null){
			uris.addAll(Cache.TEXT.get().parse(base, comments, callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
			uris.addAll(Cache.TEXT.get().parse(base, content, callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
		}
		uris.remove(null);
		count.accept(uris.size());

		return uris;
	}

	private static Stream<Node> flatten(Node parent){
		if(parent.childNodeSize() == 0 || parent.nodeName().equalsIgnoreCase("svg"))
			return Stream.of(parent);
		return Stream.concat(Stream.of(parent), parent.childNodes().stream().flatMap(HTMLParser::flatten));
	}

	URI resolve(URL base, String target) throws URISyntaxException, MalformedURLException{
		// Filter bracket selectors
		if((target.startsWith("{[") && target.endsWith("]}")) ||
				(target.startsWith("[[") && target.endsWith("]]")))
			return null;
		// Filter anchors
		if(target.charAt(0) == '#') return null;
		if(target.startsWith("//"))
			return new URI(base.getProtocol(), target, null);
		return new URL(RegexBasedParser.finesse(base, target, true)).toURI();
	}
}
