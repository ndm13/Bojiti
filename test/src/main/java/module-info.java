module net.miscfolder.bojiti.test {
	// "support" dependencies
	requires net.miscfolder.bojiti.core;
	requires net.miscfolder.bojiti.downloader.jdk;
	requires net.miscfolder.bojiti.downloader.protopack;
	requires net.miscfolder.parser.jsoup;
	requires net.miscfolder.parser.regex;
	// "external" dependencies
	requires net.miscfolder.roxyproxy;
	requires net.miscfolder.protopack;
	requires java.logging;

	uses net.miscfolder.bojiti.parser.Parser;
	uses net.miscfolder.bojiti.downloader.Downloader;
}