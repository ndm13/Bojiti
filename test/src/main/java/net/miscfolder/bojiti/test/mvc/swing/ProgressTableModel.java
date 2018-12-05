package net.miscfolder.bojiti.test.mvc.swing;

import net.miscfolder.bojiti.crawler.DownloadEventListener;
import net.miscfolder.bojiti.downloader.Response;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ProgressTableModel extends ListTableModel<Response.Progress> implements DownloadEventListener{
	private final Map<URI, Response.Progress> map = new ConcurrentHashMap<>();
	private static final Map<Response.Progress, JProgressBar> progressMap = new ConcurrentHashMap<>();
	private static final JProgressBar indeterminate = new JProgressBar(){
		@Override
		public boolean isDisplayable(){
			return true;
		}
		@Override
		public boolean isIndeterminate(){
			return true;
		}
		@Override
		public void repaint(){
			super.repaint();
			if(table != null) table.repaint();
		}
	};
	private final Set<URI> removed = ConcurrentHashMap.newKeySet();
	private final Object lock = new Object();
	private static JTable table; // FIXME THIS IS BAD

	@SuppressWarnings("unchecked")
	public ProgressTableModel(){
		super(
				new String[]{
						"Type",
						"Charset",
						"URL",
						"Progress",
						"Downloaded",
						"Estimated",
						"Accessed"
				},
				new Class<?>[]{
						String.class,
						Charset.class,
						URL.class,
						JProgressBar.class,
						Long.class,
						Long.class,
						LocalDateTime.class
				},
				new Function[]{
						f(Response.Progress::getContentType),
						f(Response.Progress::getCharset),
						f(Response.Progress::getURL),
						f((Response.Progress p)->{
							if(p.getEstimatedSize() == -1) return indeterminate;
							JProgressBar bar = progressMap.getOrDefault(p, new JProgressBar());
							bar.setMaximum((int)p.getEstimatedSize());
							bar.setValue(Math.max(0, (int)p.getDownloadedSize()));
							return bar;
						}),
						f(Response.Progress::getDownloadedSize),
						f(Response.Progress::getEstimatedSize),
						f(Response.Progress::getAccessTime)
				});
	}

	private static Response.Progress buildDummyProgress(URI uri){
		return new DummyProgress(uri);
	}

	@Override
	public void onBegin(URI uri){
		onUpdate(uri, buildDummyProgress(uri));
	}


	private volatile long lastUpdate = System.currentTimeMillis();
	@Override
	public void onUpdate(URI uri, Response.Progress progress){
		long check = lastUpdate = System.currentTimeMillis();
		SwingUtilities.invokeLater(()-> {
			if(check != lastUpdate) return;
			if(removed.contains(uri)) return;
			synchronized(lock){
				if(removed.contains(uri)) return;
				Response.Progress old = map.put(uri, progress);
				if(old == null){
					add(progress);
				}else{
					replace(old, progress);
				}
			}
		});
	}

	@Override
	public void onComplete(URI uri, Response response){
		onFailure(uri);
	}

	@Override
	public void onFailure(URI uri){
		SwingUtilities.invokeLater(()-> {
			Response.Progress old;
			synchronized(lock){
				removed.add(uri);
				old = map.remove(uri);
				if(old == null) return;
				remove(old);
			}
			progressMap.remove(old);
		});
	}

	public void setTableForProgressRepaint(JTable table){
		this.table = table;
	}

	private static class DummyProgress implements Response.Progress{
		private final URI uri;

		public DummyProgress(URI uri){
			this.uri = uri;
		}

		@Override
		public URL getURL(){
			try{
				return uri.toURL();
			}catch(MalformedURLException e){
				return null;
			}
		}

		@Override
		public Charset getCharset(){
			return Charset.defaultCharset();
		}

		@Override
		public String getContentType(){
			return "...";
		}

		@Override
		public LocalDateTime getAccessTime(){
			return LocalDateTime.MIN;
		}

		@Override
		public long getEstimatedSize(){
			return -1;
		}

		@Override
		public long getDownloadedSize(){
			return -1;
		}

		@Override
		public long getAverageSpeed(){
			return -1;
		}

		@Override
		public long getCurrentSpeed(){
			return -1;
		}
	}
}
