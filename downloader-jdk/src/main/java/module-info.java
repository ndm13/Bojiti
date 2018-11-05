import net.miscfolder.bojiti.downloader.jdk.HttpDownloader;

module net.miscfolder.bojiti.downloader.jdk {
	requires net.miscfolder.bojiti.core;
	provides net.miscfolder.bojiti.downloader.Downloader with HttpDownloader;
}