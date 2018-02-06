package net.miscfolder.bojiti.internal;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Announcer<L>{
	Set<L> listeners();

	default void addListener(L listener){
		listeners().add(listener);
	}
	default void removeListener(L listener){
		listeners().remove(listener);
	}
	default void announce(Consumer<L> action){
		CompletableFuture.runAsync(()->listeners().forEach(action));
	}
}
