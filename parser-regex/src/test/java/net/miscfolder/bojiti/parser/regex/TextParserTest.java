package net.miscfolder.bojiti.parser.regex;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TextParserTest{
	static TextParser parser;
	static URL host;
	static StringBuilder corpus;

	@BeforeAll
	static void init() throws MalformedURLException{
		parser = new TextParser();
		host = new URL("http://www.google.com/");
		corpus = new StringBuilder()
				.append("This is a bunch of sentences. We will be trying to extract ")
				.append("URLs from this text.  Regardless of spacing of periods, if ")
				.append("the sentence is correctly capitalized, we won't care.  This ")
				.append("should eliminate a lot of the false positives, but still ")
				.append("pick up things like site dot domain, hidden . domain, and ")
				.append("this (dot) that, not to mention me [[at]] site [[dot]] com. ")

				.append("Because of the way capitalization is set up, WEBSITE . NET ")
				.append("should still be parsed correctly. ")

				.append("With the new slash deobfuscation, we should be able to parse ")
				.append("things like my dot site dot com slash index dot php, as well ")
				.append("as site .com /page.aspx.");
	}

	@Test
	void parse(){
		// TODO not an actual "test"
		parser.parse(host, corpus).forEach(System.out::println);
	}
}