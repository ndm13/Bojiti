package net.miscfolder.bojiti.test.support;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.function.Supplier;

public class TimeoutSet<E> extends AbstractSet<E>{
	private final Map<E,Instant> map;
	private final TemporalAmount timeout;
	public TimeoutSet(Supplier<Map<E,Instant>> backboneSource, TemporalAmount timeout){
		map = backboneSource.get();
		this.timeout = timeout;
	}

	@Override
	public int size(){
		return map.size();
	}

	@Override
	public boolean isEmpty(){
		return map.isEmpty();
	}

	private Instant checkTimeout(E e, Instant i){
		if(e == null || i == null || i.plus(timeout).isAfter(Instant.now())) return null;
		return i;
	}

	@Override
	@SuppressWarnings("unchecked")  // Will throw exception by design
	public boolean contains(Object o){
		return null != map.compute((E)o,this::checkTimeout);
	}

	@Override
	public Iterator<E> iterator(){
		return new Iterator<E>(){
			Iterator<Map.Entry<E,Instant>> iterator = map.entrySet().iterator();
			Map.Entry<E,Instant> next;
			volatile boolean stale = true;

			@Override
			public boolean hasNext(){
				if(!stale) return next != null;
				stale = false;
				while(iterator.hasNext()){
					next = iterator.next();
					if(next.getValue().plus(timeout).isAfter(Instant.now()))
						return true;
				}
				next = null;
				return false;
			}

			@Override
			public E next(){
				if(stale && !hasNext()) throw new NoSuchElementException();
				stale = true;
				return next.getKey();
			}
		};
	}

	@Override
	public Spliterator<E> spliterator(){
		return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.IMMUTABLE & Spliterator.DISTINCT);
	}

	@SuppressWarnings("SimplifyStreamApiCallChains")    // Because stream uses the spliterator to populate
	@Override
	public Object[] toArray(){
		return stream().toArray();
	}

	@SuppressWarnings({"SimplifyStreamApiCallChains", "unchecked"})    // Because stream uses the spliterator to populate; will throw by design
	@Override
	public <T> T[] toArray(T[] a){
		return (T[]) stream().toArray();
	}

	@Override
	public boolean add(E e){
		return null != map.put(e, Instant.now().plus(timeout));
	}

	@Override
	public boolean remove(Object o){
		return null != map.remove(o);
	}

	@Override
	public void clear(){
		map.clear();
	}
}
