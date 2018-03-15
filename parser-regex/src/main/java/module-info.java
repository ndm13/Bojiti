import net.miscfolder.bojiti.parser.regex.TextParser;
import net.miscfolder.bojiti.parser.regex.CssParser;

module parser.regex{
	requires core;

	exports net.miscfolder.bojiti.parser.regex;

	provides net.miscfolder.bojiti.parser.Parser with
			TextParser, CssParser;
}