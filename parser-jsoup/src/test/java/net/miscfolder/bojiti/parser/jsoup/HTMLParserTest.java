package net.miscfolder.bojiti.parser.jsoup;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HTMLParserTest{
	private static URL url;
	private static String source;
	private static HTMLParser parser;

	private Document document;
	private URL documentBase;

	@BeforeAll
	static void init() throws IOException{
		url = new URL("https://en.wikipedia.org/wiki/Chaosnet");
		parser = new HTMLParser();
		source = scanURLToString(url);
	}

	@BeforeEach
	void setUp(){
		document = Jsoup.parse(source);
		documentBase = parser.resolve(url, document.baseUri());
		if(documentBase == null) documentBase = url;
	}

	@Test
	void parseWithFlattenedNodeChain(){
		parser.parseWithFlattenedNodeChain(url, documentBase, document);
	}

	private static String scanURLToString(URL url) throws IOException{
		Scanner s = new Scanner(url.openStream()).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}
}