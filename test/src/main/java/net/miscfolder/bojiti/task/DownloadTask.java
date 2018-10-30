package net.miscfolder.bojiti.task;

import java.io.IOException;
import java.net.URL;

import javafx.concurrent.Task;
import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.RedirectionException;
import net.miscfolder.bojiti.downloader.Response;

public class DownloadTask extends Task<Response> implements Downloader.Listener{
	private final URL url;
	private Response response;

	public DownloadTask(URL url){
		this.url = url;
		setOnFailed(e->{
			updateTitle("Exception*");
			updateMessage(getException().getLocalizedMessage());
		});
	}

	@Override
	protected Response call() throws IOException, RedirectionException{
		updateTitle("Connecting");
		updateMessage(url.toExternalForm());
		updateProgress(-1, 0);
		Downloader downloader = Downloader.SPI.getFirst(url.getProtocol());
		if(downloader == null)
			throw new IllegalArgumentException("No downloader for " + url.getProtocol());
		try{
			downloader.addEventListener(this);
			downloader.download(url);
		}finally{
			downloader.removeEventListener(this);
		}
		if(response == null){
			updateTitle("Exception");
			updateMessage("Unknown error");
			updateProgress(0,1);
		}
		return response;
	}

	@Override
	public void onDownloadStarted(Response response){
		if(this.response == null &&
				url.toExternalForm().equals(response.getURL().toExternalForm())){
			this.response = response;
			updateValue(response);
			updateTitle("Downloading");
		}
	}

	@Override
	public void onDownloadUpdate(Response response){
		if(response.equals(this.response)){
			updateValue(response);
			updateProgress(response.getDownloadedSize(), response.getEstimatedSize());
		}
	}

	@Override
	public void onDownloadComplete(Response response){
		if(response.equals(this.response)){
			updateValue(response);
			updateTitle("Complete");
			updateProgress(response.getDownloadedSize(), response.getDownloadedSize());
		}
	}

	@Override
	public void onDownloadError(Response response, Exception exception){
		if(response.equals(this.response)){
			updateValue(response);
			updateTitle("Exception");
			updateMessage(exception.getLocalizedMessage());
			setException(exception);
		}
	}
}
