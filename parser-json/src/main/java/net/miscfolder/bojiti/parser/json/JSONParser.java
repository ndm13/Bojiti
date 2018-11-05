package net.miscfolder.bojiti.parser.json;

import com.google.gson.*;
import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.bojiti.support.CharBufferReader;

import java.io.CharArrayReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@MimeTypes({"application/json", "text/json"})
public class JSONParser implements Parser{
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback){
		JsonStreamParser parser = new JsonStreamParser(findReader(chars));
		Set<URI> uris = ConcurrentHashMap.newKeySet();
		while(parser.hasNext()){
			JsonElement element = parser.next();
			uris.addAll(parse(url, element, callback));
		}
		return uris;
	}

	public Set<URI> parse(URL url, JsonElement element, Consumer<ParserException> callback){
		if(element.isJsonArray())
			return parse(url, element.getAsJsonArray(), callback);
		if(element.isJsonObject())
			return parse(url, element.getAsJsonObject(), callback);
		if(element.isJsonPrimitive())
			return parse(url, element.getAsJsonPrimitive(), callback);
		return Set.of();
	}

	public Set<URI> parse(URL url, JsonPrimitive primitive, Consumer<ParserException> callback){
		if(!primitive.isString()) // the only one we care about really
			return Set.of();
		try{
			return Parser.SPI.getFirst("text/plain").parse(url, primitive.getAsString(), callback);
		}catch(NoSuchElementException e){
			// Nothing we can do...
			return Set.of();
		}
	}

	public Set<URI> parse(URL url, JsonObject object, Consumer<ParserException> callback){
		Set<URI> uris = new HashSet<>();
		for(Map.Entry<String,JsonElement> pair : object.entrySet())
			uris.addAll(parse(url, pair.getValue(), callback));
		return uris;
	}

	public Set<URI> parse(URL url, JsonArray array, Consumer<ParserException> callback){
		Set<URI> uris = new HashSet<>();
		for(JsonElement element : array)
			uris.addAll(parse(url, element, callback));
		return uris;
	}

	private static Reader findReader(CharSequence sequence){
		if(sequence instanceof CharBuffer){
			CharBuffer buffer = (CharBuffer) sequence;
			if(buffer.hasArray())
				return new CharArrayReader(buffer.array());
			return new CharBufferReader(buffer);
		}
		if(sequence instanceof String)
			return new StringReader((String) sequence);
		return new StringReader(sequence.toString());
	}
}
