module net.miscfolder.bojiti.parser.rfcmessage {
	requires net.miscfolder.bojiti.core;
	requires apache.mime4j.core;
	requires apache.mime4j.dom;

	requires net.miscfolder.protopack;
	requires net.miscfolder.bojiti.parser.regex;

	exports net.miscfolder.bojiti.parser.rfcmessage;

	provides net.miscfolder.bojiti.parser.Parser with
			net.miscfolder.bojiti.parser.rfcmessage.RFC822Parser;
}