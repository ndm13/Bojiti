package net.miscfolder.bojiti.support;

import java.util.EventListener;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public interface Dispatcher<L extends EventListener>{
	/**
	 * A set that contains all the event listeners attached
	 * to this object.
	 *
	 * @return  A {@link Set} of event listeners ({@link L})
	 */
	Set<L> getEventListeners();

	/**
	 * The {@link Executor} that handles event propagation.
	 *
	 * Defaults to {@link ForkJoinPool#commonPool()}.  Note
	 * that this Executor DOES NOT guarantee ordering!
	 *
	 * @see     #dispatch(Consumer) for more information on
	 *          how events are handled.
	 * @return  The Executor that will handle the events.
	 */
	default Executor getEventExecutor(){
		return ForkJoinPool.commonPool();
	}

	/**
	 * Adds an event listener to this object.  All events
	 * announced to this object will be received by the
	 * provided listener ({@link L}) until it is
	 * {@link #removeEventListener(EventListener) removed}.
	 *
	 * Defaults to {@link #getEventListeners()}.{@link Set#add(Object) add(listener)}.
	 *
	 * @param listener
	 *          The event listener to add.
	 */
	default void addEventListener(L listener){
		getEventListeners().add(listener);
	}

	/**
	 * Removes an event listener from this object.  Any
	 * events received after this action occurs will not be
	 * forwarded to this listener on a best-effort basis.
	 *
	 * Defaults to {@link #getEventListeners()}.{@link Set#remove(Object) remove(listener)}.
	 *
	 * @param listener
	 *          The event listener to remove.
	 */
	default void removeEventListener(L listener){
		getEventListeners().remove(listener);
	}

	/**
	 * Forwards an event to all listeners currently in the
	 * Set received from {@link #getEventListeners()}.  The
	 * design intent is that, for a listener in the form
	 * {@code void onUpdate(Data data);}, this method can
	 * be invoked on all listeners by calling
	 * {@code dispatch(l->l.onUpdate(data));}.
	 *
	 * The default implementation submits a new call to
	 * {@link Consumer#accept(Object)} to the {@link Executor}
	 * provided by {@link #getEventExecutor()} for each
	 * event listener ({@link L}) in the backing collection
	 * ({@link #getEventListeners()}).
	 *
	 * @param action
	 */
	default void dispatch(Consumer<L> action){
		getEventListeners().forEach(l->getEventExecutor().execute(()->action.accept(l)));
	}
}
