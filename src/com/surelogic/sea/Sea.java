package com.surelogic.sea;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a <i>sea</i> of knowledge, not intended to be subclassed. Sea
 * instances contain and manage <i>drops</i> of information. Instances form a
 * truth maintenance system by managing dependent and deponent drops.
 * <p>
 * This class is thread-safe.
 * 
 * @see Drop
 */
public final class Sea {

	/**
	 * Returns the default sea of knowledge.
	 * 
	 * @return the default sea of knowledge
	 */
	public static Sea getDefault() {
		return f_defaultSea.get();
	}

	/**
	 * Gets the set of all valid drops in this sea.
	 * 
	 * @return the set of all valid drops in this sea.
	 */
	public Set<Drop> getDrops() {
		f_validDropsLock.readLock().lock();
		try {
			return new HashSet<Drop>(f_validDrops);
		} finally {
			f_validDropsLock.readLock().unlock();
		}
	}

	/**
	 * Returns the set of drops within this sea that are of
	 * <code>dropType</code> or any of its subtypes.
	 * <p>
	 * Typical use would be to extract all drops in the sea that are assignment
	 * compatible with a specific type, as shown in the below code snippet.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *    class MySubDrop extends MyDrop { ... }
	 *    Drop d1 = new Drop();
	 *    MyDrop d2 = new MyDrop();
	 *    MySubDrop d3 = new MySubDrop();
	 *      
	 *    Sea.getDefault().getDropsOfType(Drop.class) = { d1 , d2, d3 }
	 *    Sea.getDefault().getDropsOfType(MyDrop.class) = { d2, d3 }
	 *    Sea.getDefault().getDropsOfType(MySubDrop.class) = { d3 }
	 * </pre>
	 * 
	 * @param dropType
	 *            the type of drops desired.
	 * @return the set of drops found.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public <T extends Drop> Set<T> getDropsOfType(Class<T> dropType) {
		f_validDropsLock.readLock().lock();
		try {
			return filterDropsOfType(dropType, f_validDrops);
		} finally {
			f_validDropsLock.readLock().unlock();
		}
	}

	/**
	 * Returns the set of drops within <code>dropSet</code> that are of
	 * <code>dropType</code> or any of its subtypes.
	 * <p>
	 * Typical use would be to subset a set of drops such that all drops in the
	 * subset are assignment compatible with a specific type, as shown in the
	 * below code snippet.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *    class MySubDrop extends MyDrop { ... }
	 *    Drop d1 = new Drop();
	 *    MyDrop d2 = new MyDrop();
	 *    MySubDrop d3 = new MySubDrop();
	 *      
	 *    Set&lt;Drop&gt; r = Sea.getDefault().getDrops();
	 *    (NOTE) r = { d1, d2, d3 }
	 *    Set&lt;Drop&gt; r1 = Sea.filterDropsOfType(Drop.class, r);
	 *    (NOTE) r1 = { d1, d2, d3 }
	 *    Set&lt;MySubDrop&gt; r2 = Sea.filterDropsOfType(MySubDrop.class, r);
	 *    (NOTE) r2 = { d3 }
	 * </pre>
	 * 
	 * @param dropType
	 *            the type of drops desired.
	 * @param dropSet
	 *            the set of drops to subset. This set is not modified.
	 * @return the set of drops found.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Drop> Set<T> filterDropsOfType(Class<T> dropType,
			Set<Drop> dropSet) {
		if (dropType == null)
			throw new IllegalArgumentException("type must be non-null");
		if (dropSet == null)
			throw new IllegalArgumentException("dropSet must be non-null");
		final Set<T> result = new HashSet<T>();
		for (Drop drop : dropSet) {
			if (dropType.isInstance(drop)) {
				result.add((T) drop);
			}
		}
		return result;
	}

	/**
	 * Mutates <code>mutableDropSet</code> removing all drops from it that are
	 * not of <code>dropType</code> or any of its subtypes. This method
	 * returns a references to the mutated set that is up-cast (the client is
	 * warned that subsequent mutations to <code>mutableDropSet</code> via the
	 * reference passed to this method could invalidate the up-cast).
	 * <p>
	 * Due to the up-cast, this method is less "safe" than
	 * {@link #filterDropsOfType(Class, Set)}, however, it can improve
	 * performance by avoiding creating a copy if the original drop set is no
	 * longer needed.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *    class MySubDrop extends MyDrop { ... }
	 *    Drop d1 = new Drop();
	 *    MyDrop d2 = new MyDrop();
	 *    MySubDrop d3 = new MySubDrop();
	 *      
	 *    Set&lt;Drop&gt; r = Sea.getDefault().getDrops();
	 *    (NOTE) r = { d1, d2, d3 }
	 *    Set&lt;MySubDrop&gt; r2 = Sea.filterDropsOfTypeMutate(MySubDrop.class, r);
	 *    (NOTE) r2 = { d3 }
	 *    (NOTE) r2.equals(r)
	 *    r.add(d1); // bad! set mutation violates up-cast
	 *    for (MySubDrop d : r2) { ... } // throws a ClassCastException
	 * </pre>
	 * 
	 * @param dropType
	 *            the type of drops desired.
	 * @param mutableDropSet
	 *            the set of drops to mutate.
	 * @return an up-cast reference to <code>mutableDropSet</code>.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Drop> Set<T> filterDropsOfTypeMutate(
			Class<T> dropType, Set<Drop> mutableDropSet) {
		if (dropType == null)
			throw new IllegalArgumentException("type must be non-null");
		if (mutableDropSet == null)
			throw new IllegalArgumentException(
					"mutableDropSet must be non-null");
		for (Iterator<Drop> i = mutableDropSet.iterator(); i.hasNext();) {
			Drop drop = i.next();
			if (!dropType.isInstance(drop)) {
				i.remove();
			}
		}
		return (Set<T>) mutableDropSet;
	}

	/**
	 * Returns the set of drops within this sea that are of
	 * <code>dropType</code> (subtypes are <i>not</i> included).
	 * <p>
	 * Typical use would be to extract all drops in the sea that are of a
	 * specific type, as shown in the below code snippet.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *    class MySubDrop extends MyDrop { ... }
	 *    Drop d1 = new Drop();
	 *    MyDrop d2 = new MyDrop();
	 *    MySubDrop d3 = new MySubDrop();
	 *      
	 *    Sea.getDefault().getDropsOfExactType(Drop.class) = { d1 }
	 *    Sea.getDefault().getDropsOfExactType(MyDrop.class) = { d2 }
	 *    Sea.getDefault().getDropsOfExactType(MySubDrop.class) = { d3 }
	 * </pre>
	 * 
	 * @param dropType
	 *            the type of drops to look for in the sea
	 * @return the set of drops found
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public <T extends Drop> Set<T> getDropsOfExactType(Class<T> dropType) {
		f_validDropsLock.readLock().lock();
		try {
			return filterDropsOfExactType(dropType, f_validDrops);
		} finally {
			f_validDropsLock.readLock().unlock();
		}
	}

	/**
	 * Returns the set of drops within <code>dropSet</code> that are of
	 * <code>dropType</code> (subtypes are <i>not</i> included).
	 * <p>
	 * Typical use would be to subset a set of drops such that all drops in the
	 * subset are of a specific type, as shown in the below code snippet.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *    class MySubDrop extends MyDrop { ... }
	 *    Drop d1 = new Drop();
	 *    MyDrop d2 = new MyDrop();
	 *    MySubDrop d3 = new MySubDrop();
	 *      
	 *    Set&lt;Drop&gt; r = Sea.getDefault().getDrops();
	 *    (NOTE) r = { d1, d2, d3 }
	 *    Set&lt;Drop&gt; r1 = Sea.filterDropsOfExactType(Drop.class, r);
	 *    (NOTE) r1 = { d1 }
	 *    Set&lt;MySubDrop&gt; r2 = Sea.filterDropsOfExactType(MySubDrop.class, r);
	 *    (NOTE) r2 = { d3 }
	 * </pre>
	 * 
	 * @param dropType
	 *            the exact type of drops desired.
	 * @param dropSet
	 *            the set of drops to subset. This set is not modified.
	 * @return the set of drops found
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Drop> Set<T> filterDropsOfExactType(
			Class<T> dropType, Set<Drop> dropSet) {
		if (dropType == null)
			throw new IllegalArgumentException("type must be non-null");
		if (dropSet == null)
			throw new IllegalArgumentException("dropSet must be non-null");
		final Set<T> result = new HashSet<T>();
		for (Drop drop : dropSet) {
			if (drop.getClass().equals(dropType)) {
				T dr = (T) drop;
				result.add(dr);
			}
		}
		return result;
	}

	/**
	 * Mutates <code>mutableDropSet</code> removing all drops from it that are
	 * not of <code>dropType</code> (subtypes are <i>not</i> included). This
	 * method returns a references to the mutated set that is up-cast (the
	 * client is warned that subsequent mutations to <code>mutableDropSet</code>
	 * via the reference passed to this method could invalidate the up-cast).
	 * <p>
	 * Due to the up-cast, this method is less "safe" than
	 * {@link #filterDropsOfExactType(Class, Set)}, however, it can improve
	 * performance by avoiding creating a copy if the original drop set is no
	 * longer needed.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *    class MySubDrop extends MyDrop { ... }
	 *    Drop d1 = new Drop();
	 *    MyDrop d2 = new MyDrop();
	 *    MySubDrop d3 = new MySubDrop();
	 *      
	 *    Set&lt;Drop&gt; r = Sea.getDefault().getDrops();
	 *    (NOTE) r = { d1, d2, d3 }
	 *    Set&lt;MyDrop&gt; r2 = Sea.filterDropsOfExactTypeMutate(MyDrop.class, r);
	 *    (NOTE) r2 = { d2 }
	 *    (NOTE) r2.equals(r)
	 *    r.add(d1); // bad! set mutation violates up-cast
	 *    for (MyDrop d : r2) { ... } // throws a ClassCastException
	 * </pre>
	 * 
	 * @param dropType
	 *            the type of drops desired.
	 * @param mutableDropSet
	 *            the set of drops to mutate.
	 * @return an up-cast reference to <code>mutableDropSet</code>.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Drop> Set<T> filterDropsOfExactTypeMutate(
			Class<T> dropType, Set<Drop> mutableDropSet) {
		if (dropType == null)
			throw new IllegalArgumentException("type must be non-null");
		if (mutableDropSet == null)
			throw new IllegalArgumentException(
					"mutableDropSet must be non-null");
		for (Iterator<Drop> i = mutableDropSet.iterator(); i.hasNext();) {
			Drop drop = i.next();
			if (!drop.getClass().equals(dropType)) {
				i.remove();
			}
		}
		return (Set<T>) mutableDropSet;
	}

	/**
	 * Returns the set of drops within <code>dropSet</code> that match the
	 * given drop predicate.
	 * 
	 * @param pred
	 *            the drop predicate to apply to the drop set.
	 * @param dropSet
	 *            the set of drops to subset. This set is not modified.
	 * @return the set of drops in <code>dropSet</code> that matched the drop
	 *         predicate.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public static <T extends Drop> Set<T> filter(DropPredicate pred,
			Set<T> dropSet) {
		if (pred == null)
			throw new IllegalArgumentException(
					"drop predicate must be non-null");
		if (dropSet == null)
			throw new IllegalArgumentException("dropSet must be non-null");
		final Set<T> result = new HashSet<T>(dropSet);
		filterMutate(pred, result);
		return result;
	}

	/**
	 * Mutates the given drop set by removing all drops within it that do not
	 * match the given drop predicate.
	 * 
	 * @param pred
	 *            the drop predicate to apply to the drop set.
	 * @param mutableDropSet
	 *            the set of drops to mutate.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public static <T extends Drop> void filterMutate(DropPredicate pred,
			Set<T> mutableDropSet) {
		if (pred == null)
			throw new IllegalArgumentException(
					"drop predicate must be non-null");
		if (mutableDropSet == null)
			throw new IllegalArgumentException(
					"mutableDropSet must be non-null");
		for (Iterator<T> i = mutableDropSet.iterator(); i.hasNext();) {
			Drop drop = i.next();
			if (!pred.match(drop)) {
				i.remove();
			}
		}
	}

	/**
	 * Queries if at least one drop in the given set is matched by the given
	 * drop predicate.
	 * 
	 * @param pred
	 *            the drop predicate to use.
	 * @param dropSet
	 *            the drop set to examine.
	 * @return <code>true</code> if at least one drop matches,
	 *         <code>false</code> otherwise.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public static boolean hasMatchingDrops(DropPredicate pred,
			Set<? extends Drop> dropSet) {
		if (pred == null)
			throw new IllegalArgumentException(
					"drop predicate must be non-null");
		if (dropSet == null)
			throw new IllegalArgumentException("dropSet must be non-null");
		for (Drop drop : dropSet) {
			if (pred.match(drop)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds references to those drops in the source set that match the given
	 * drop predicate into the result set.
	 * 
	 * @param sourceDropSet
	 *            the source drop set. This set is not modified.
	 * @param pred
	 *            the drop predicate to apply to the source drop set.
	 * @param mutableResultDropSet
	 *            the result set to add matching drops into.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public static void addMatchingDropsFrom(Set<? extends Drop> sourceDropSet,
			DropPredicate pred, Set<Drop> mutableResultDropSet) {
		if (sourceDropSet == null)
			throw new IllegalArgumentException("sourceDropSet must be non-null");
		if (pred == null)
			throw new IllegalArgumentException(
					"drop predicate must be non-null");
		if (mutableResultDropSet == null)
			throw new IllegalArgumentException(
					"mutableResultDropSet must be non-null");
		for (Drop drop : sourceDropSet) {
			if (pred.match(drop)) {
				mutableResultDropSet.add(drop);
			}
		}
	}

	/**
	 * Registers an observer interested in status changes to drops in this sea.
	 * 
	 * @param observer
	 *            the concrete observer object.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public <T extends Drop> void register(DropObserver observer) {
		if (observer == null)
			throw new IllegalArgumentException("observer must be non-null");
		f_observers.add(observer);
	}

	/**
	 * Removes an observer that is no longer interested in status changes to
	 * drops in this sea.
	 * <p>
	 * If the observer had not registered with this sea then this call has no
	 * effect.
	 * 
	 * @param observer
	 *            the concrete observer object.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public <T extends Drop> void unregister(DropObserver observer) {
		if (observer == null)
			throw new IllegalArgumentException("observer must be non-null");
		f_observers.remove(observer);
	}

	/**
	 * Invalidates all drops contained within this sea. Acts as a
	 * straightforward reset method to invalidate all that is currently known.
	 */
	public void invalidateAll() {
		// we need to make a copy of the set of drops in the sea as the set will
		// be changing (rapidly) as we invalidate drops within it
		final Set<Drop> safeCopy = new HashSet<Drop>();
		f_validDropsLock.readLock().lock();
		try {
			safeCopy.addAll(f_validDrops);
		} finally {
			f_validDropsLock.readLock().unlock();
		}
		for (Drop drop : safeCopy) {
			drop.invalidate();
		}
	}

	/**
	 * Invalidates all drops contained within this sea that match the given drop
	 * predicate.
	 * 
	 * @param pred
	 *            The predicate to match drops against.
	 */
	public void invalidateMatching(DropPredicate pred) {
		// we need to make a copy of the set of drops in the sea as the set will
		// be changing (rapidly) as we invalidate drops within it
		final Set<Drop> safeCopy = new HashSet<Drop>();
		f_validDropsLock.readLock().lock();
		try {
			addMatchingDropsFrom(f_validDrops, pred, safeCopy);
		} finally {
			f_validDropsLock.readLock().unlock();
		}
		// safeCopy.addAll(validDropSet);
		for (Iterator<Drop> i = safeCopy.iterator(); i.hasNext();) {
			Drop drop = i.next();
			drop.invalidate();
		}
	}

	/**
	 * Notification to this sea that something about the knowledge status of a
	 * drop has changed. This method should only be invoked from the
	 * {@link Drop} class. This method orchestrates change notifications back to
	 * clients code listeners.
	 * 
	 * @param drop
	 *            the drop the notification is about.
	 * @param event
	 *            what happened to the drop.
	 */
	void notify(Drop drop, DropEvent event) {
		f_version.incrementAndGet();

		if (event == DropEvent.Created) {
			// add the new drop to this sea's list of valid drops
			f_validDropsLock.writeLock().lock();
			try {
				f_validDrops.add(drop);
			} finally {
				f_validDropsLock.writeLock().unlock();
			}
		} else if (event == DropEvent.Invalidated) {
			// remove the drop from this sea's list of valid drops
			f_validDropsLock.writeLock().lock();
			try {
				f_validDrops.remove(drop);
			} finally {
				f_validDropsLock.writeLock().unlock();
			}
		}

		// notify all of the registered observers of the status change
		for (DropObserver observer : f_observers) {
			observer.dropChanged(drop, event);
		}
	}

	/**
	 * The version number of this sea. This number increments each time a change
	 * to the knowledge status of any drop within this sea occurs.
	 * <p>
	 * The version number could wrap, so it might not be strictly increasing.
	 * 
	 * @return the version number of this sea.
	 */
	public long getVersion() {
		return f_version.get();
	}

	/**
	 * The default sea.
	 */
	private static final AtomicReference<Sea> f_defaultSea = new AtomicReference<Sea>(
			new Sea());

	/**
	 * A version number that is incremented each time a change to the knowledge
	 * status of any drop within this sea occurs.
	 */
	private AtomicLong f_version = new AtomicLong(Long.MIN_VALUE);

	/**
	 * The set of valid drops within this sea.
	 */
	private final Set<Drop> f_validDrops = new HashSet<Drop>(5000);

	/**
	 * Protects {@link #f_validDrops}.
	 */
	private final ReadWriteLock f_validDropsLock = new ReentrantReadWriteLock();

	/**
	 * The set of observers interested in changes to the knowledge status of
	 * drops in this sea.
	 */
	private final Set<DropObserver> f_observers = new CopyOnWriteArraySet<DropObserver>();
}