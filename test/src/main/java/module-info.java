module test {
	// Java dependencies
	requires javafx.graphics;
	requires javafx.controls;
	exports net.miscfolder.bojiti.mvc.view to javafx.graphics;
	exports net.miscfolder.bojiti.task to javafx.graphics;
	// "support" dependencies
	requires core;
	requires downloader.jdk;
	requires downloader.protopack;
	requires parser.jsoup;
	requires parser.regex;
	// "external" dependencies
	requires roxyproxy;
	requires protopack;
	requires java.desktop;
}