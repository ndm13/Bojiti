package net.miscfolder.bojiti.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public abstract class URLConnectionDownloader implements Downloader{
	protected Response download(URLConnection connection, InputStream stream, Consumer<Response.Progress> callback)
			throws IOException{
		if(stream == null) throw new IllegalArgumentException("Stream must be non-null");
		Response.Builder builder = new Response.Builder(connection);
		callback.accept(builder);
		if(isInfiniteStream(connection)){
			return builder.completeInfinite();
		}

		int read;
		byte[] buffer = new byte[Response.DEFAULT_BUFFER_SIZE];
		long start = System.currentTimeMillis();
		while((read = stream.read(buffer)) > -1){
			long end = System.currentTimeMillis();
			callback.accept(builder.update(buffer, read, end - start));
			if(Thread.currentThread().isInterrupted()){
				throw new InterruptedIOException("Thread interrupted during download");
			}
			start = end;
		}
		return builder.complete();
	}

	protected static String guessContentType(URLConnection connection){
		return connection.getContentType() != null ?
				connection.getContentType() :
				URLConnection.guessContentTypeFromName(connection.getURL().toExternalForm());
	}

	protected static Charset guessCharset(URLConnection connection){
		try{
			// Ideally we can just use the header
			return Charset.forName(connection.getHeaderField("charset"));
		}catch(IllegalArgumentException ignore){}

		// Maybe they set it in the content type
		String contentType = connection.getContentType();
		if(contentType != null){
			int charsetIndex;
			if((charsetIndex = contentType.indexOf("charset=")) > 0){
				try{
					int end = contentType.indexOf(';', charsetIndex);
					if(end == -1) end = contentType.length();
					String extracted = contentType.substring(charsetIndex + 8, end);
					while(extracted.charAt(0) == '"')
						extracted = extracted.substring(1);
					while(extracted.charAt(extracted.length() - 1) == '"')
						extracted = extracted.substring(0, extracted.length() - 1);
					return Charset.forName(extracted.toUpperCase());
				}catch(IllegalArgumentException ignore){}
			}
		}

		// No other options without adding another dependency and
		// increasing overhead.  IETF says US-ASCII.  TODO maybe?
		return StandardCharsets.US_ASCII;
	}

	/**
	 * Ported from Theo->Checker#isInfiniteStream()
	 *
	 * Attempts to determine if a URLConnection is infinite, running the following checks:
	 * - If the connection length is non-zero or non-negative, return false
	 * - If the transfer encoding is chunked, return false
	 * - If the content type is application/octet-stream, return false
	 * - If the content type contains audio or video, return true
	 * - Else, return false
	 *
	 * @param connection    The connection to test.
	 * @return              true if the conditions are met, false otherwise.
	 */
	protected static boolean isInfiniteStream(URLConnection connection){
		// Check content length (if set, it's probably finite)
		if(connection.getContentLengthLong() > 0) return false;

		// Check for chunked data stream (if it isn't, it's probably finite)
		String encodingHeader = connection.getHeaderField("Transfer-Encoding");
		if(encodingHeader != null && !"chunked".equals(
				encodingHeader.toLowerCase().trim()))
			return false;

		String type = connection.getContentType();

		if(type != null){
			type = type.toLowerCase().trim();

			// Check if it's an octet stream (if it is, it's probably a progressive fetch)
			if(type.contains("application/octet-stream")) return false;

			// Check if it's audio or video (if it is, it's probably a stream)
			if(type.contains("audio") || type.contains("video") || type.startsWith("application/ogg")) return true;
		}
		// Otherwise, we don't really know, so we'll assume it's not.
		return false;
	}

}
