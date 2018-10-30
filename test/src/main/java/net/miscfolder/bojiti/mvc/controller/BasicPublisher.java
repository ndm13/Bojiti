package net.miscfolder.bojiti.mvc.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

public class BasicPublisher<T> implements Flow.Publisher<T>{
	private Map<Flow.Subscriber<? super T>,AtomicLong> subscribers = new HashMap<>();

	protected void push(T item){
		subscribers.forEach((s,c)->{
			if(c.updateAndGet(l->l==0?0:l==Long.MAX_VALUE?l:l--) > 0)
				s.onNext(item);
		});
	}

	protected void error(Throwable error){
		subscribers.keySet().forEach(s->s.onError(error));
	}

	protected void close(){
		subscribers.keySet().forEach(Flow.Subscriber::onComplete);
		subscribers.clear();
	}

	@Override
	public void subscribe(Flow.Subscriber<? super T> subscriber){
		subscribers.put(subscriber, new AtomicLong());
		subscriber.onSubscribe(new Flow.Subscription(){
			@Override
			public void request(long n){
				if(n <= 0){
					subscriber.onError(new IllegalArgumentException("Request must be > 0"));
					return;
				}
				subscribers.get(subscriber)
						.updateAndGet(l->Long.MAX_VALUE - l > n ? Long.MAX_VALUE : l + n);
			}
			@Override
			public void cancel(){
				subscribers.remove(subscriber);
			}
		});
	}
}
