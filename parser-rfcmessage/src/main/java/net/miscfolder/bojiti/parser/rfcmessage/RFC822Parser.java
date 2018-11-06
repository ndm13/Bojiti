package net.miscfolder.bojiti.parser.rfcmessage;

import net.miscfolder.bojiti.downloader.Response;
import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;
import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.bojiti.parser.rfcmessage.support.NoLeadingNewlineInputStream;
import net.miscfolder.bojiti.support.CharBufferReader;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.DefaultMessageBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

@MimeTypes({"message/rfc822"})
public class RFC822Parser implements Parser{
	@Override
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback){
		Set<URI> uris = new HashSet<>();
		MessageBuilder builder = new DefaultMessageBuilder();
		try{
			Message message = builder.parseMessage(
					new NoLeadingNewlineInputStream(
							streamFromReader(CharBufferReader.findReader(chars))));
			uris.addAll(parse(url, message.getTo(), callback));
			uris.addAll(parse(url, message.getReplyTo(), callback));
			uris.addAll(parse(url, message.getFrom(), callback));
			uris.addAll(parse(url, message.getCc(), callback));
			uris.addAll(parse(url, message.getBcc(), callback));
			uris.addAll(parse(url, message.getSender(), callback));
			uris.addAll(parse(url, message.getBody(), message.getMimeType(), callback));
		}catch(IOException | MimeException e){
			callback.accept(new ParserException("Couldn't parse MIME message", e));
		}
		return uris;
	}

	public Set<URI> parse(URL url, Body body, String mimeType, Consumer<ParserException> callback){
		if(body == null) return Set.of();
		Set<URI> uris = new HashSet<>();
		if(body instanceof Multipart){
			Multipart multipart = (Multipart) body;
			try{
				Parser textParser = Parser.SPI.getFirst("text/plain");
				if(multipart.getPreamble() != null)
					uris.addAll(textParser.parse(url, multipart.getPreamble(), callback));
				if(multipart.getEpilogue() != null)
					uris.addAll(textParser.parse(url, multipart.getEpilogue(), callback));
				for(Entity part : multipart.getBodyParts()){
					uris.addAll(parse(url, part.getBody(), part.getMimeType(), callback));
				}
			}catch(NoSuchElementException ignore){}
		}else if(body instanceof TextBody){
			try{
				Parser parser = Parser.SPI.getFirst(Response.getBasicContentType(mimeType));
				try{
					byte[] bytes = ((TextBody) body).getInputStream().readAllBytes();
					uris.addAll(parser.parse(url, new String(bytes, ((TextBody) body).getMimeCharset()), callback));
				}catch(IOException e){
					callback.accept(new ParserException("Error reading stream of sub-part", e));
				}
			}catch(NoSuchElementException ignore){}
		}
		return uris;
	}

	public Set<URI> parse(URL url, AddressList addresses, Consumer<ParserException> callback){
		if(addresses == null) return Set.of();
		Set<URI> uris = new HashSet<>();
		for(Address address : addresses){
			if(address instanceof Mailbox)
				uris.addAll(parse(url, (Mailbox) address, callback));
			else
				uris.add(URI.create("mailto:" + address.toString()));
		}
		return uris;
	}

	public Set<URI> parse(URL url, MailboxList mailboxes, Consumer<ParserException> callback){
		if(mailboxes == null) return Set.of();
		return mailboxes.stream().map(m->parse(url, m, callback))
				.reduce((a,b)->{a.addAll(b);return a;}).orElseGet(Set::of);
	}

	public Set<URI> parse(URL url, Mailbox mailbox, Consumer<ParserException> callback){
		Set<URI> uris = new HashSet<>();
		if(mailbox == null) return Set.of();
		if(mailbox.getDomain() != null){
			if(mailbox.getLocalPart() != null)
				uris.add(URI.create("mailto:" + mailbox.getLocalPart() + "@" + mailbox.getDomain()));
		}
		if(mailbox.getRoute() != null)
			mailbox.getRoute().forEach(s -> {
				uris.add(URI.create("smtp://" + s));
			});
		return uris;
	}

	private static InputStream streamFromReader(Reader reader){
		return new InputStream(){
			@Override
			public int read() throws IOException{
				return reader.read();
			}
		};
	}
}
