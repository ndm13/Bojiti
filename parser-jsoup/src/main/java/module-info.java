import net.miscfolder.bojiti.parser.jsoup.AngularTemplateParser;
import net.miscfolder.bojiti.parser.jsoup.HTMLParser;

module net.miscfolder.bojiti.parser.jsoup {
	requires net.miscfolder.bojiti.core;
	requires net.miscfolder.bojiti.parser.regex;
	requires jsoup;

	provides net.miscfolder.bojiti.parser.Parser with
			HTMLParser, AngularTemplateParser;
}