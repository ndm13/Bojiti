package net.miscfolder.bojiti.parser.protopack;

import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.protopack.ProtoPack;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@MimeTypes("text/x-gopher-menu")
public class GopherMenuParser implements Parser{
	static{ProtoPack.install();}

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
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback){
		StringBuilder text = new StringBuilder();
		Set<URI> uris = new HashSet<>();
		for(String line : GOPHER_NEWLINE_PATTERN.split(chars)){
			if(line.length() > 1){
				String[] parts = line.split("\t");
				try{
					// BUGFIX - Some servers don't tab-separate part identifier
					if(parts.length == 4){
						parts = new String[]{String.valueOf(parts[0].charAt(0)), parts[0].substring(1), parts[1],
									parts[2], parts[3]};
					}
					char itemType = parts[0].charAt(0);
					String userDisplayString = parts[1],
							selector = parts[2],
							hostname = parts[3];
					text.append(userDisplayString).append('\n');

					int port = Integer.parseInt(parts[4]);

					if(hostname.startsWith("URL:")){
						uris.add(new URI(hostname.substring(4)));
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
						uris.add(new URI(protocol, null, hostname,
								port != 70 ? port : -1,
								"/" + itemType + selector,
								null, null));
					}
				}catch(URISyntaxException e){
					callback.accept(new InvalidMenuItemException(parts, "URL non-resolvable", e));
				}catch(IndexOutOfBoundsException | NumberFormatException e){
					callback.accept(new InvalidMenuItemException(parts, "Menu data invalid", e));
				}
			}
		}

		Parser textParser = Parser.SPI.getFirst("text/plain");
		if(textParser != null){
			// Now that we have the greedy text parser, we need to remove
			// same-domain+protocol+port URLs because they probably have
			// the wrong slashtype
			try{
				URI current = url.toURI();
				for(URI uri : textParser.parse(url, text.toString(), callback)){
					if(!uri.getHost().equals(current.getHost()) ||
							!uri.getScheme().equals(current.getScheme()) ||
							uri.getPort() != current.getPort()){
						uris.add(uri);
					}
				}
			}catch(URISyntaxException ignore){

			}
		}

		return uris;
	}
}
