import net.miscfolder.bojiti.downloader.jdk.HttpDownloader;

module downloader.jdk {
	requires core;
	provides net.miscfolder.bojiti.downloader.Downloader with HttpDownloader;
}