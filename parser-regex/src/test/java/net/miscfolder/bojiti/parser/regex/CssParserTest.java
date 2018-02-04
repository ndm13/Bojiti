package net.miscfolder.bojiti.parser.regex;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import net.miscfolder.protopack.ProtoPack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CssParserTest{
	@BeforeAll
	static void setup(){
		// Allows us to use data: as a protocol
		// Suppresses errors in test
		ProtoPack.install();
	}

	@Test
	void parse() throws IOException{
		CssParser parser = new CssParser();
		URL url = new URL(YT_CSS_SOURCE);
		String source = quickDownload(url);
		for(URL found : parser.parse(url, source)){
			System.out.println(found.toExternalForm());
		}
	}

	private static String quickDownload(URL url) throws IOException{
		return new Scanner(url.openStream()).useDelimiter("\\A").next();
	}

	private static final String YT_CSS_SOURCE =
			"https://www.youtube.com/yts/cssbin/player-vflRSN4l1/www-player-2x.css";
}