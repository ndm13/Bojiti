package net.miscfolder.bojiti;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import net.miscfolder.bojiti.downloader.Downloader;
import net.miscfolder.bojiti.downloader.Protocols;
import net.miscfolder.bojiti.parser.MimeTypes;
import net.miscfolder.bojiti.parser.Parser;

public final class SPI<T,A extends Annotation>{
	public static final SPI<Downloader,Protocols> Downloaders =
			new SPI<>(Downloader.class, Protocols.class, Protocols::value);
	public static final SPI<Parser,MimeTypes> Parsers =
			new SPI<>(Parser.class, MimeTypes.class, MimeTypes::value);

	private final Map<String,Deque<T>> cache = new ConcurrentHashMap<>();
	private final Class<T> typeClass;
	private final Class<A> annotationClass;
	private final Function<A, String[]> keyGenerator;

	private SPI(Class<T> typeClass, Class<A> keyAnnotation,
			Function<A,String[]> keyGenerator){
		this.typeClass = typeClass;
		this.annotationClass = keyAnnotation;
		this.keyGenerator = keyGenerator;
		reload();
	}

	/**
	 * Returns the first provider for the given key, or null if there isn't one
	 * available.
	 *
	 * @param key   the key to get a provider for
	 * @return      the first provider available
	 */
	public T getFirst(String key){
		Iterator<T> iterator = get(key).iterator();
		if(!iterator.hasNext()){
			// TODO log like a competent person
			System.err.println("Requested identifier has no implementation: " + key);
			return null;
		}
		return iterator.next();
	}

	/**
	 * Gets all providers for the supplied key.  If the key has no providers
	 * associated with it, an {@link Collections#emptyList() empty List} is
	 * returned instead.
	 *
	 * To get the first provider in the collection, we recommend using
	 * {@link #getFirst(String)} instead.
	 *
	 * @param key   the key to get providers for
	 * @return      a {@link Collection} of providers
	 */
	public Collection<T> get(String key){
		Deque<T> cached = cache.get(key);
		return cached == null ? Collections.emptyList() :
				Collections.unmodifiableCollection(cached);
	}

	/**
	 * Sets the provided service provider as the first in the list for the
	 * provided key.  If this provider is present, it is removed before
	 * adding it to the front.
	 *
	 * @param key   the key to prioritize the provider for
	 * @param spi   the provider to prioritize
	 */
	public void prioritize(String key, T spi){
		cache.compute(key, (p,q)->{
			q.remove(spi);
			q.addFirst(spi);
			return q;
		});
	}

	/**
	 * A snapshot of the currently stored cache, wrapped in
	 * {@link Collections#unmodifiableMap(Map)}.
	 *
	 * @return a read-only version of the current cache
	 */
	public Map<String,Deque<T>> getCacheSnapshot(){
		return Collections.unmodifiableMap(cache);
	}

	/**
	 * Reloads the service providers using {@link ServiceLoader#load(Class)},
	 * generating a mapping to the loaded providers based on the key function
	 * supplied in the constructor.
	 *
	 * This method is synchronized because ServiceLoader may not always be
	 * thread-safe.
	 */
	public synchronized void reload(){
		ServiceLoader.load(typeClass)
				.stream()
				.filter(p->p.type().isAnnotationPresent(annotationClass))
				.map(p->Map.entry(p, keyGenerator.apply(p.type().getAnnotation(annotationClass))))
				.forEach(entry->{
					T t = entry.getKey().get();
					for(String key : entry.getValue())
						cache.compute(key,(proto,set)->{
							if(set == null) set = new ArrayDeque<>();
							set.add(t);
							return set;
						});
				});
	}
}
