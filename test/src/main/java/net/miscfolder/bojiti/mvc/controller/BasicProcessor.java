package net.miscfolder.bojiti.mvc.controller;

import java.util.concurrent.Flow;

public class BasicProcessor<T,R> extends BasicPublisher<R> implements Flow.Processor<T,R>{
	@Override
	public void onSubscribe(Flow.Subscription subscription){

	}

	@Override
	public void onNext(T item){

	}

	@Override
	public void onError(Throwable throwable){

	}

	@Override
	public void onComplete(){

	}
}
