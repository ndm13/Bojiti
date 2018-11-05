module net.miscfolder.bojiti.parser.json {
	requires net.miscfolder.bojiti.core;
	requires gson;

	provides net.miscfolder.bojiti.parser.Parser with
			net.miscfolder.bojiti.parser.json.JSONParser;
}