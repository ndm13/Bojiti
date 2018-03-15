import net.miscfolder.bojiti.parser.jsoup.AngularTemplateParser;
import net.miscfolder.bojiti.parser.jsoup.HTMLParser;

module parser.jsoup{
	requires core;
	requires parser.regex;
	requires jsoup;

	provides net.miscfolder.bojiti.parser.Parser with
			HTMLParser, AngularTemplateParser;
}