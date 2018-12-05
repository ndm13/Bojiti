package net.miscfolder.bojiti.crawler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractCrawler implements Crawler{
	private final Object startLock = new Object();
	private volatile boolean started = false;

	protected final Set<DownloadEventListener> downloadEventListeners = ConcurrentHashMap.newKeySet();
	protected final Set<ParseEventListener> parseEventListeners = ConcurrentHashMap.newKeySet();

	private Thread daemon = null;

	@Override
	public boolean isStarted(){
		synchronized(startLock){
			return started;
		}
	}

	@Override
	public void start(){
		if(started) return;
		synchronized(startLock){
			if(started) return;
			daemon = new Thread(this);
			daemon.setName(this.getClass().getCanonicalName() + "-Daemon-Thread");
			daemon.setDaemon(true);
			daemon.start();
			started = true;
		}
	}

	@Override
	public void shutdown() throws TerminationException{
		if(!started) return;
		synchronized(startLock){
			if(!started) return;
			if(daemon != null && daemon.isAlive()){
				daemon.interrupt();
				try{
					daemon.join();
				}catch(InterruptedException e){
					throw new TerminationException("Interrupted during termination", e);
				}
			}
			daemon = null;
			started = false;
		}
	}

	@Override
	public void addDownloadEventListener(DownloadEventListener listener){
		downloadEventListeners.add(listener);
	}

	@Override
	public void removeDownloadEventListener(DownloadEventListener listener){
		downloadEventListeners.remove(listener);
	}

	@Override
	public void addParseEventListener(ParseEventListener listener){
		parseEventListeners.add(listener);
	}

	@Override
	public void removeParseEventListener(ParseEventListener listener){
		parseEventListeners.remove(listener);
	}
}
