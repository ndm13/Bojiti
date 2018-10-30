package net.miscfolder.bojiti.downloader;

import net.miscfolder.bojiti.support.Dispatcher;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Response implements Dispatcher<Response.Listener>{
	public static final String CONTENT_HASH_TYPE = "SHA-256";
	public static final int DEFAULT_BUFFER_SIZE = 1024;

	// Always going to be time of instantiation
	private final LocalDateTime accessed = LocalDateTime.now();

	private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

	// Properties from connection itself
	private final URL url;
	private final Charset charset;
	private final String contentType;
	private final long estimatedSize;

	// Properties updated during download
	private AtomicInteger
			currentBytes = new AtomicInteger(),
			totalBytes = new AtomicInteger();
	private AtomicLong
			currentMS = new AtomicLong(),
			totalMS = new AtomicLong();

	// Properties generated upon completion
	private boolean complete = false;
	private byte[] contentHash;

	public Response(URLConnection connection){
		this(connection.getURL(),
				URLConnectionDownloader.guessCharset(connection),
				connection.getContentType(),
				connection.getContentLengthLong());
	}

	public Response(URL url, Charset charset, String contentType, long estimatedSize){
		this.url = url;
		this.charset = charset;
		this.estimatedSize = estimatedSize;

		if(contentType == null){
			this.contentType = URLConnection.guessContentTypeFromName(url.toExternalForm());
		}else{
			this.contentType = contentType;
		}
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

	public CharBuffer getContent(){
		return charset.decode(ByteBuffer.wrap(byteStream.toByteArray()));
	}

	public LocalDateTime getAccessed(){
		return accessed;
	}

	public byte[] getContentHash(){
		return contentHash;
	}

	public long getEstimatedSize(){
		return estimatedSize;
	}

	public int getDownloadedSize(){
		return totalBytes.get();
	}

	public long getAverageSpeed(){
		return (totalBytes.get() * 8) / Math.max(totalMS.get(), 1);
	}

	public long getCurrentSpeed(){
		return (currentBytes.get() * 8) / Math.max(currentMS.get(), 1);
	}

	public boolean isComplete(){
		return complete;
	}

	public ByteArrayOutputStream getByteStream(){
		return complete ? null : byteStream;
	}

	public void updateSpeed(int bytes, long ms){
		if(!complete){
			ForkJoinPool.commonPool().execute(()->{
				currentBytes.set(bytes);
				currentMS.set(ms);
				totalBytes.addAndGet(bytes);
				totalMS.addAndGet(ms);
				dispatch(Listener::onUpdate);
			});
		}
	}

	public synchronized Response complete(byte[] contentHash){
		if(!complete){
			this.contentHash = contentHash;
			complete = true;
		}
		dispatch(Listener::onComplete);
		return this;
	}

	public synchronized Response completeInfinite(byte[] emptyHash){
		if(!complete){
			complete(emptyHash);
			totalBytes.set(-1);
			totalMS.set(1);
			currentBytes.set(-1);
			currentBytes.set(1);
		}
		dispatch(Listener::onComplete);
		return this;
	}

	private Set<Listener> listeners = new CopyOnWriteArraySet<>();

	@Override
	public Set<Listener> getEventListeners(){
		return listeners;
	}

	public interface Listener extends EventListener{
		void onUpdate();
		void onComplete();
		void onError(Exception e);
	}
}
