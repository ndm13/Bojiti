module parser.jsoup{
	requires core;
	requires jsoup;

	provides net.miscfolder.bojiti.parser.Parser with
			net.miscfolder.bojiti.parser.jsoup.HTMLParser;
}