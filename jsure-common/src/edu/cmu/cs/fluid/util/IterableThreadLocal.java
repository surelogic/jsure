package edu.cmu.cs.fluid.util;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A version of ThreadLocal that keeps references to its contents
 * so they can be operated on when not accessed concurrently
 * 
 * @author Edwin
 */
public abstract class IterableThreadLocal<T> extends ThreadLocal<T> implements Iterable<T> {
	private final List<T> tracked = new CopyOnWriteArrayList<T>();
	
	@Override
	protected final T initialValue() {
		T val = makeInitialValue();
		tracked.add(val);
		return val;
	}

	protected abstract T makeInitialValue();
	
	private final void stopTrackingOldValue() {
		T old = get();
		tracked.remove(old);
	}
	
	// Should be atomic, since they're thread-local
	public final void remove() {
		stopTrackingOldValue();
		remove();
	}
	
	// Should be atomic, since they're thread-local
	public final void set(T newVal) {
		stopTrackingOldValue();		
		set(newVal);
		tracked.add(newVal);
	}
	
	// Not thread-safe unless the calling thread has done a join with all the relevant threads
	public Iterator<T> iterator() {
		return tracked.iterator();
	}
}
