module parser.regex{
	requires core;

	provides net.miscfolder.bojiti.parser.Parser with
			net.miscfolder.bojiti.parser.regex.TextParser;
}