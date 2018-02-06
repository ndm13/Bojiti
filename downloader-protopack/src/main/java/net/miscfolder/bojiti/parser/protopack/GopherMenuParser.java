package net.miscfolder.bojiti.parser.protopack;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.miscfolder.bojiti.worker.SPI;
import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;

@MimeTypes("text/x-gopher-menu")
public class GopherMenuParser extends Parser{
	private static final Pattern GOPHER_NEWLINE_PATTERN = Pattern.compile("\r\n");

	// Menu types that require another protocol prefix
	private static final char
			CCSO_NAMESERVER = '2',
			TELNET = '8',
			TELNET_3270 = 'T';

	// Menu types that have no links
	private static final char
			GOPHER_INFO = 'i',
			GOPHER_ERROR = '3';

	@Override
	public Set<URL> parse(URL url, CharSequence chars){
		StringBuilder text = new StringBuilder();
		Set<URL> urls = new HashSet<>();
		for(String line : GOPHER_NEWLINE_PATTERN.split(chars)){
			if(line.length() > 1){
				String[] parts = line.split("\t");
				try{
					char itemType = parts[0].charAt(0);
					String userDisplayString = parts[1],
							selector = parts[2],
							hostname = parts[3];
					text.append(userDisplayString).append('\n');

					int port = Integer.parseInt(parts[4]);

					if(hostname.startsWith("URL:")){
						urls.add(new URL(hostname.substring(4)));
					}else if(itemType != GOPHER_ERROR && itemType != GOPHER_INFO){
						String protocol;
						switch(itemType){
							case CCSO_NAMESERVER:
								protocol = "ph";
								break;
							case TELNET:
							case TELNET_3270:
								protocol = "telnet";
								break;
							default:
								protocol = "gopher";
								break;
						}
						if(hostname.startsWith("GET /")){
							selector = hostname.substring(4);
							protocol = "http";
						}
						if(!selector.startsWith("/")){
							selector = '/' + selector;
						}
						urls.add(new URL(protocol, hostname, port != 70 ? port : -1,
								"/" + itemType + selector));
					}
				}catch(MalformedURLException e){
					announce(l->l.onParserError(url,
							new InvalidMenuItemException(parts, "URL non-resolvable", e)));
				}catch(IndexOutOfBoundsException | NumberFormatException e){
					announce(l->l.onParserError(url,
							new InvalidMenuItemException(parts, "Menu data invalid", e)));
				}
			}
		}

		Parser textParser = SPI.Parsers.getFirst("text/plain");
		if(textParser != null)
			urls.addAll(textParser.parse(url, text.toString()));

		return urls;
	}
}
