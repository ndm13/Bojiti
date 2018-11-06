import net.miscfolder.bojiti.parser.json.JSONParser;

module net.miscfolder.bojiti.parser.json {
	requires net.miscfolder.bojiti.core;
	requires gson;

	exports net.miscfolder.bojiti.parser.json;

	provides net.miscfolder.bojiti.parser.Parser with
			JSONParser;
}