package net.miscfolder.bojiti.parser.json;

import com.google.gson.*;
import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.bojiti.support.CharBufferReader;

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@MimeTypes({"application/json", "application/ld+json", "text/json", "text/ld+json"})
public class JSONParser implements Parser{
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback, IntConsumer count){
		JsonStreamParser parser = new JsonStreamParser(CharBufferReader.findReader(chars));
		Set<URI> uris = ConcurrentHashMap.newKeySet();
		while(parser.hasNext()){
			JsonElement element = parser.next();
			uris.addAll(parse(url, element, callback, i->count.accept(uris.size() + i)));
		}
		count.accept(uris.size());
		return uris;
	}

	public Set<URI> parse(URL url, JsonElement element, Consumer<ParserException> callback, IntConsumer count){
		if(element.isJsonArray())
			return parse(url, element.getAsJsonArray(), callback, count);
		if(element.isJsonObject())
			return parse(url, element.getAsJsonObject(), callback, count);
		if(element.isJsonPrimitive())
			return parse(url, element.getAsJsonPrimitive(), callback, count);
		return Set.of();
	}

	public Set<URI> parse(URL url, JsonPrimitive primitive, Consumer<ParserException> callback, IntConsumer count){
		if(!primitive.isString()) // the only one we care about really
			return Set.of();
		try{
			Set<URI> uris = Parser.SPI.getFirst("text/plain").parse(url, primitive.getAsString(), callback, count);
			count.accept(uris.size());
			return uris;
		}catch(NoSuchElementException e){
			// Nothing we can do...
			return Set.of();
		}
	}

	public Set<URI> parse(URL url, JsonObject object, Consumer<ParserException> callback, IntConsumer count){
		Set<URI> uris = new HashSet<>();
		for(Map.Entry<String,JsonElement> pair : object.entrySet())
			uris.addAll(parse(url, pair.getValue(), callback, i->count.accept(uris.size() + i)));
		count.accept(uris.size());
		return uris;
	}

	public Set<URI> parse(URL url, JsonArray array, Consumer<ParserException> callback, IntConsumer count){
		Set<URI> uris = new HashSet<>();
		for(JsonElement element : array)
			uris.addAll(parse(url, element, callback, i->count.accept(uris.size() + i)));
		count.accept(uris.size());
		return uris;
	}

}
