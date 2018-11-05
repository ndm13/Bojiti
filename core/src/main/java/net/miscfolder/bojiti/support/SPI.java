package net.miscfolder.bojiti.support;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SPI<T,A extends Annotation>{
	private final Map<String,Deque<T>> cache = new ConcurrentHashMap<>();
	private final Set<String> misses = ConcurrentHashMap.newKeySet();
	private final Class<T> typeClass;
	private final Class<A> annotationClass;
	private final Function<A, String[]> keyGenerator;

	private volatile boolean logMisses = false;

	public SPI(Class<T> typeClass, Class<A> keyAnnotation, Function<A, String[]> keyGenerator){
		this.typeClass = typeClass;
		this.annotationClass = keyAnnotation;
		this.keyGenerator = keyGenerator;
		reload();
	}

	/**
	 * Returns a {@link Set} of all failed lookups, as long as
	 * {@link #setLoggingMisses(boolean)} logMisses} is true.
	 *
	 * @return      a Set of all lookups that weren't resolved while logMisses
	 *              was enabled
	 */
	public Set<String> getLoggedMisses(){
		return Collections.unmodifiableSet(misses);
	}

	/**
	 * Enable or disable logging of failed lookups.
	 *
	 * @param logMisses true if enabled, false if disabled.
	 * @see #getLoggedMisses()
	 */
	public void setLoggingMisses(boolean logMisses){
		this.logMisses = logMisses;
	}

	/**
	 * Returns the state of the lookup miss logger.
	 *
	 * @return  true if logging failed lookups, false otherwise
	 * @see #getLoggedMisses()
	 */
	public boolean isLoggingMisses(){
		return logMisses;
	}

	/**
	 * Returns the first provider for the given key, or null if there isn't one
	 * available.
	 *
	 * @param keys  the keys to get a provider for
	 * @return      the first provider available
	 * @throws NoSuchElementException
	 *              if there's no provider for the key
	 */
	public T getFirst(String... keys){
		for(String key : keys){
			Iterator<T> iterator = get(key).iterator();
			if(iterator.hasNext()){
				return iterator.next();
			}
			if(logMisses) misses.add(key);
		}
		throw new NoSuchElementException("Requested identifiers have no implementation: " + Arrays.toString(keys));
	}


	/**
	 * Gets all providers for the supplied key.  If the key has no providers
	 * associated with it, an {@link Collections#emptyList() empty List} is
	 * returned instead.
	 *
	 * To get the first provider in the collection, we recommend using
	 * {@link #getFirst(String...)} instead.
	 *
	 * @param key   the key to get providers for
	 * @return      a {@link Deque} of providers
	 */
	public Collection<T> get(String key){
		Deque<T> cached = cache.get(key);
		if(cached == null){
			if(logMisses) misses.add(key);
			return Collections.emptySet();
		}
		return Collections.unmodifiableCollection(cached);
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
			if(q != null){
				q.remove(spi);
			}else{
				q = new LinkedList<>();
			}
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
