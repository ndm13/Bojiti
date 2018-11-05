import net.miscfolder.bojiti.parser.regex.TextParser;
import net.miscfolder.bojiti.parser.regex.CssParser;

module net.miscfolder.parser.regex{
	requires net.miscfolder.bojiti.core;

	exports net.miscfolder.bojiti.parser.regex;

	provides net.miscfolder.bojiti.parser.Parser with
			TextParser, CssParser;
}