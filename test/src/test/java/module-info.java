module test {
	// "internal" dependencies
	requires core;
	requires downloader.jdk;
	requires downloader.protopack;
	requires parser.jsoup;
	requires parser.regex;
	// "external" dependencies
	requires roxyproxy;
	requires protopack;
}