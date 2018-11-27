module net.miscfolder.bojiti.test {
	// "support" dependencies
	requires net.miscfolder.bojiti.core;
	requires net.miscfolder.bojiti.downloader.jdk;
	requires net.miscfolder.bojiti.downloader.protopack;
	requires net.miscfolder.bojiti.parser.jsoup;
	requires net.miscfolder.bojiti.parser.regex;
	requires net.miscfolder.bojiti.parser.json;
	requires net.miscfolder.bojiti.parser.rfcmessage;
	// "external" dependencies
	requires net.miscfolder.roxyproxy;
	requires net.miscfolder.protopack;
	requires java.logging;
	requires java.desktop;

	requires minidns.core;
	requires minidns.client;

	uses net.miscfolder.bojiti.parser.Parser;
	uses net.miscfolder.bojiti.downloader.Downloader;
}