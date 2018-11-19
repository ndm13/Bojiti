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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@MimeTypes({"message/rfc822"})
public class RFC822Parser implements Parser{
	@Override
	public Set<URI> parse(URL url, CharSequence chars, Consumer<ParserException> callback, IntConsumer count){
		Set<URI> uris = new HashSet<>();
		MessageBuilder builder = new DefaultMessageBuilder();
		try{
			Message message = builder.parseMessage(
					new NoLeadingNewlineInputStream(
							streamFromReader(CharBufferReader.findReader(chars))));
			chars = null;   // GC
			uris.addAll(parse(url, message.getTo(), callback, count));
			count.accept(uris.size());
			uris.addAll(parse(url, message.getReplyTo(), callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
			uris.addAll(parse(url, message.getFrom(), callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
			uris.addAll(parse(url, message.getCc(), callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
			uris.addAll(parse(url, message.getBcc(), callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
			uris.addAll(parse(url, message.getSender(), callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
			uris.addAll(parse(url, message.getBody(), message.getMimeType(), callback, i->count.accept(uris.size() + i)));
			count.accept(uris.size());
		}catch(IOException | MimeException e){
			callback.accept(new ParserException("Couldn't parse MIME message", e));
		}
		return uris;
	}

	public Set<URI> parse(URL url, Body body, String mimeType, Consumer<ParserException> callback, IntConsumer count){
		if(body == null) return Set.of();
		Set<URI> uris = new HashSet<>();
		if(body instanceof Multipart){
			Multipart multipart = (Multipart) body;
			try{
				Parser textParser = Parser.SPI.getFirst("text/plain");
				if(multipart.getPreamble() != null)
					uris.addAll(textParser.parse(url, multipart.getPreamble(), callback, i->count.accept(uris.size() + i)));
				if(multipart.getEpilogue() != null)
					uris.addAll(textParser.parse(url, multipart.getEpilogue(), callback, i->count.accept(uris.size() + i)));
				count.accept(uris.size());
				for(Entity part : multipart.getBodyParts()){
					uris.addAll(parse(url, part.getBody(), part.getMimeType(), callback, i->count.accept(uris.size() + i)));
					count.accept(uris.size());
				}
			}catch(NoSuchElementException ignore){}
		}else if(body instanceof TextBody){
			try{
				Parser parser = Parser.SPI.getFirst(Response.getBasicContentType(mimeType));
				try{
					byte[] bytes = ((TextBody) body).getInputStream().readAllBytes();
					uris.addAll(parser.parse(url, new String(bytes, ((TextBody) body).getMimeCharset()), callback,
							i->count.accept(uris.size() + i)));
				}catch(IOException e){
					callback.accept(new ParserException("Error reading stream of sub-part", e));
				}
				count.accept(uris.size());
			}catch(NoSuchElementException ignore){}
		}
		return uris;
	}

	public Set<URI> parse(URL url, AddressList addresses, Consumer<ParserException> callback, IntConsumer count){
		if(addresses == null) return Set.of();
		Set<URI> uris = new HashSet<>();
		for(Address address : addresses){
			if(address instanceof Mailbox)
				uris.addAll(parse(url, (Mailbox) address, callback, i->count.accept(uris.size() + i)));
			else
				uris.add(URI.create("mailto:" + address.toString()));
			count.accept(uris.size());
		}
		return uris;
	}

	public Set<URI> parse(URL url, MailboxList mailboxes, Consumer<ParserException> callback, IntConsumer count){
		if(mailboxes == null) return Set.of();
		AtomicInteger ai = new AtomicInteger();
		return mailboxes.stream().map(m->parse(url, m, callback, i->count.accept(ai.addAndGet(i))))
				.reduce((a,b)->{a.addAll(b);return a;}).orElseGet(Set::of);
	}

	public Set<URI> parse(URL url, Mailbox mailbox, Consumer<ParserException> callback, IntConsumer count){
		Set<URI> uris = new HashSet<>();
		if(mailbox == null) return Set.of();
		if(mailbox.getDomain() != null){
			if(mailbox.getLocalPart() != null)
				uris.add(URI.create("mailto:" + mailbox.getLocalPart() + "@" + mailbox.getDomain()));
		}
		count.accept(uris.size());
		if(mailbox.getRoute() != null)
			mailbox.getRoute().forEach(s -> {
				uris.add(URI.create("smtp://" + s));
			});
		count.accept(uris.size());
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
