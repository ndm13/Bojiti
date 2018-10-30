package net.miscfolder.bojiti.downloader;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class URLConnectionDownloader implements Downloader{
	protected void download(Response response, URLConnection connection, InputStream stream)
			throws IOException, NoSuchAlgorithmException{
		MessageDigest digest = MessageDigest.getInstance(Response.CONTENT_HASH_TYPE);
		if(isInfiniteStream(connection)){
			response.completeInfinite(digest.digest());
			return;
		}

		try(BufferedInputStream inputStream = new BufferedInputStream(new DigestInputStream(stream, digest))){
			int read;
			byte[] buffer = new byte[Response.DEFAULT_BUFFER_SIZE];
			ByteArrayOutputStream outputStream = response.getByteStream();
			long start = System.currentTimeMillis();
			while((read = inputStream.read(buffer)) > -1){
				outputStream.write(buffer, 0, read);
				long end = System.currentTimeMillis();
				response.updateSpeed(read, end - start);
				if(Thread.currentThread().isInterrupted()){
					throw new InterruptedIOException("Thread interrupted during download");
				}
				start = end;
			}
			response.complete(digest.digest());
		}
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
					String extracted = contentType.substring(charsetIndex + 7, end);
					return Charset.forName(extracted);
				}catch(IllegalArgumentException ignore){}
			}
		}

		// No other options without adding another dependency and
		// increasing overhead.  IETF says US-ASCII.  TODO maybe?
		return Charset.forName("US-ASCII");
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
