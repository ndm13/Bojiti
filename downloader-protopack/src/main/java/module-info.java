import net.miscfolder.bojiti.downloader.protopack.*;
import net.miscfolder.bojiti.parser.protopack.*;

module net.miscfolder.bojiti.downloader.protopack{
	requires net.miscfolder.bojiti.core;
	requires net.miscfolder.protopack;
	provides net.miscfolder.bojiti.downloader.Downloader with
			DataDownloader, GopherDownloader, JavaScriptDownloader;
	/*
	GopherDownloader outputs with mime type text/x-gopher-menu.
	This pack provides a simple parser that returns the standard
	URLs and delegates the rendered body to a text parser.
	 */
	provides net.miscfolder.bojiti.parser.Parser with
			GopherMenuParser;

}