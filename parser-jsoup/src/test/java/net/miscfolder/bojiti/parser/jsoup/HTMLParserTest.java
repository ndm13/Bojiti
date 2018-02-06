package net.miscfolder.bojiti.parser.jsoup;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;

class HTMLParserTest{
	private static URL url;
	private static String source;
	private static HTMLParser parser;

	private Document document;
	private URL documentBase;

	private static Set<URL> oldV = new HashSet<>();
	private static Set<URL> newV = new HashSet<>();

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
	@Deprecated
	void oldVersion(){
		oldV = parser.parseWithGetAllElements(url, documentBase, document);
	}

	@Test
	void newVersion(){
		newV = parser.parseWithFlattenedNodeChain(url, documentBase, document);
	}

	@AfterAll
	static void difference(){
		Set<URL> inOld = new HashSet<>(oldV);
		inOld.removeAll(newV);
		Set<URL> inNew = new HashSet<>(newV);
		inNew.removeAll(oldV);
		System.out.println("In old, but not new:");
		inOld.forEach(System.out::println);
		System.out.println("\nIn new, but not old:");
		inNew.forEach(System.out::println);
	}

	private static String scanURLToString(URL url) throws IOException{
		Scanner s = new Scanner(url.openStream()).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}
}