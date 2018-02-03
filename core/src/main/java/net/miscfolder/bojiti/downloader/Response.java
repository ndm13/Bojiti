package net.miscfolder.bojiti.downloader;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Response{
	public static final String CONTENT_HASH_TYPE = "SHA-256";
	public static final int DEFAULT_BUFFER_SIZE = 1024;

	private final Set<SpeedUpdateListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
			CompletableFuture.runAsync(()->{
				currentBytes.set(bytes);
				currentMS.set(ms);
				int totalBytes = this.totalBytes.addAndGet(bytes);
				long totalMS = this.totalMS.addAndGet(ms);
				CompletableFuture.runAsync(()->{
					long currentSpeed = (bytes * 8) / ms;
					long averageSpeed = (totalBytes * 8) / totalMS;
					listeners.forEach(l->l.onSpeedUpdate(currentSpeed, averageSpeed));
				});
			});
		}
	}

	public void addSpeedUpdateListener(SpeedUpdateListener listener){
		listeners.add(listener);
	}

	public synchronized Response complete(byte[] contentHash){
		if(!complete){
			this.contentHash = contentHash;
			complete = true;
		}
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
		return this;
	}


	@FunctionalInterface
	public interface SpeedUpdateListener{
		void onSpeedUpdate(long currentSpeed, long averageSpeed);
	}
}
