package net.miscfolder.bojiti.downloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class Response{
	// TODO config
	public static final String CONTENT_HASH_TYPE = "SHA-256";
	// TODO config
	public static final int DEFAULT_BUFFER_SIZE = 1024;

	private final URL url;
	private final Charset charset;
	private final String contentType;
	private final LocalDateTime accessed;
	private final long size, ms;
	private final byte[] contentHash;
	private final CharBuffer content;

	private Response(URL url, Charset charset, String contentType, LocalDateTime accessed, long size, long ms,
	                 byte[] contentHash, CharBuffer content){
		this.url = url;
		this.charset = charset;
		this.contentType = contentType;
		this.accessed = accessed;
		this.size = size;
		this.ms = ms;
		this.contentHash = contentHash;
		this.content = content;
	}

	public URL getURL(){
		return url;
	}

	public Charset getCharset(){
		return charset;
	}

	public String getContentType(){
		return contentType;
	}

	public String getBasicContentType(){
		return Response.getBasicContentType(contentType);
	}

	public static String getBasicContentType(String contentType){
		int semicolon = contentType.indexOf(';');
		return semicolon == -1 ? contentType : contentType.substring(0, semicolon).trim();
	}

	public LocalDateTime getAccessed(){
		return accessed;
	}

	public byte[] getContentHash(){
		return contentHash;
	}

	public CharBuffer getContent(){
		return content;
	}

	public long getSize(){
		return size;
	}

	public long getMillis(){
		return ms;
	}

	public long getAverageSpeed(){
		return (size * 8) / Math.max(ms, 1);
	}

	public interface Progress{
		URL getURL();
		Charset getCharset();
		String getContentType();
		LocalDateTime getAccessTime();
		long getEstimatedSize();
		long getDownloadedSize();
		long getAverageSpeed();
		long getCurrentSpeed();
	}

	public static class Builder implements Progress{
		private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
		private final OutputStream outputStream;
		private final MessageDigest digest;

		private final AtomicLong
				currentBytes = new AtomicLong(),
				totalBytes = new AtomicLong(),
				currentMS = new AtomicLong(),
				totalMS = new AtomicLong();

		private final URL url;
		private final Charset charset;
		private final String contentType;
		private final long estimatedSize;

		private LocalDateTime accessed;

		public Builder(URLConnection connection){
			try{
				digest = MessageDigest.getInstance(CONTENT_HASH_TYPE);
				outputStream = new DigestOutputStream(byteStream, digest);
			}catch(NoSuchAlgorithmException e){
				throw new IllegalStateException("Hard-coded algorithm doesn't exist", e);
			}

			url = connection.getURL();
			charset = URLConnectionDownloader.guessCharset(connection);
			contentType = URLConnectionDownloader.guessContentType(connection);
			estimatedSize = connection.getContentLengthLong();
		}

		public Builder update(byte[] bytes, int length, long ms){
			if(accessed == null) accessed = LocalDateTime.now();
			try{
				outputStream.write(bytes, 0, length);
			}catch(IOException e){
				throw new IllegalStateException("Output stream closed on update", e);
			}
			currentBytes.set(length);
			currentMS.set(ms);
			totalBytes.addAndGet(length);
			totalMS.addAndGet(ms);
			return this;
		}

		public Response complete(){
			return new Response(url, charset, contentType, accessed, totalBytes.get(), totalMS.get(), digest.digest(),
					charset.decode(ByteBuffer.wrap(byteStream.toByteArray())));
		}

		public Response completeInfinite(){
			return new Response(url, charset, contentType, accessed, -1, -1, digest.digest(),
					CharBuffer.allocate(0));
		}

		@Override
		public URL getURL(){
			return url;
		}

		@Override
		public Charset getCharset(){
			return charset;
		}

		@Override
		public String getContentType(){
			return contentType;
		}

		@Override
		public LocalDateTime getAccessTime(){
			return accessed;
		}

		@Override
		public long getEstimatedSize(){
			return estimatedSize;
		}

		@Override
		public long getDownloadedSize(){
			return totalBytes.get();
		}

		@Override
		public long getAverageSpeed(){
			return (totalBytes.get() * 8) / Math.max(totalMS.get(), 1);
		}

		@Override
		public long getCurrentSpeed(){
			return (currentBytes.get() * 8) / Math.max(currentMS.get(), 1);
		}

		@Override
		public String toString(){
			return accessed + "\t" + contentType + "\t" + getDownloadedSize() + "/~" + estimatedSize + " bytes@" +
					getCurrentSpeed() + "bps (avg " + getAverageSpeed() + ")\t" + url.toExternalForm();
		}
	}
}
