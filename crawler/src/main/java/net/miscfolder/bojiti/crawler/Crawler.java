package net.miscfolder.bojiti.crawler;

import java.net.URI;

public interface Crawler extends Runnable{
	/**
	 * `true` if this Crawler is currently managing a crawling task, `false`
	 * otherwise.
	 * @return whether or not this Crawler is managing a job
	 */
	boolean isStarted();

	/**
	 * If not currently managing a crawling task, tries to start a new one.  If
	 * there is an issue initializing the job, this method will throw a
	 * {@link InitializationException} with an explanation.
	 *
	 * If a task is currently running, this method returns early.  Conversely,
	 * this method will block until the job is running.
	 *
	 * @throws InitializationException if a crawler job can't be started
	 */
	void start() throws InitializationException;

	/**
	 * Tries to stop the current crawling task, if one is running.  If there is
	 * an issue terminating the job, this method will throw a
	 * {@link TerminationException} with an explanation.
	 *
	 * If a task isn't currently running, this method returns early.
	 * Conversely, this method will block until the job is stopped.
	 *
	 * @throws TerminationException if the crawler job can't be stopped
	 */
	void shutdown() throws TerminationException;

	/**
	 * Adds the provided {@link URI}s to whatever local source is in use.  This
	 * method doesn't require that these URIs be added to a backend structure,
	 * but it does imply that these URIs will at some point be downloaded and
	 * the results added to whatever sink is used for storage.
	 *
	 * @param uris  the URIs to be downloaded by this service
	 */
	void seed(URI... uris);

	void addDownloadEventListener(DownloadEventListener listener);
	void removeDownloadEventListener(DownloadEventListener listener);
	void addParseEventListener(ParseEventListener listener);
	void removeParseEventListener(ParseEventListener listener);

	/**
	 * A method that loops until interrupted.  This method will continually
	 * take new URIs from whatever source it is configured to use, then
	 * download the contents, passing events to the registered
	 * {@link DownloadEventListener}s.  If the download succeeds, the result is
	 * parsed and any parse events are sent to the registered
	 * {@link ParseEventListener)s.
	 */
	void run();
}
