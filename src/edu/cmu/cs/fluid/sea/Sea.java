package edu.cmu.cs.fluid.sea;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.concurrent.ConcurrentHashSet;
import com.surelogic.common.logging.SLLogger;

/**
 * Represents a <i>sea</i> of knowledge, not intended to be subclassed. Sea
 * instances contain and manage <i>drops</i> of information. Instances form a
 * truth maintenance system by managing dependent and deponent drops.
 * 
 * @see Drop
 */
public final class Sea {

	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("Sea");

	/**
	 * Returns the default sea of knowledge.
	 * 
	 * @return the default sea of knowledge
	 */
	public static Sea getDefault() {
		return DEFAULT_SEA;
	}

	/**
	 * Gets the set of all valid drops in this sea.
	 * 
	 * @return the set of all valid drops in this sea.
	 */
	public Set<Drop> getDrops() {
		return new HashSet<Drop>(f_validDrops);
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
		return filterDropsOfType(dropType, f_validDrops);
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
	 * not of <code>dropType</code> or any of its subtypes. This method returns
	 * a references to the mutated set that is up-cast (the client is warned
	 * that subsequent mutations to <code>mutableDropSet</code> via the
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
		return filterDropsOfExactType(dropType, f_validDrops);
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
	 * Returns the set of drops within <code>dropSet</code> that match the given
	 * drop predicate.
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
	 * Registers an observer interested in status changes to drops of a specific
	 * type and any of its subtypes.
	 * <p>
	 * Typical use would be to register a subtype of {@link Drop} that is of
	 * interest to analysis code, as shown in the below code snippet.
	 * 
	 * <pre>
	 *    class MyDrop extends Drop { ... }
	 *      
	 *    Sea.getDefault().register(MyDrop.class, new DropObserver() {
	 *      public void dropChanged(Drop drop, DropEvent event) {
	 *        // do something because a MyDrop status change has occurred
	 *      }
	 *    });
	 * </pre>
	 * 
	 * @param dropType
	 *            the drop subtype of interest.
	 * @param observer
	 *            the concrete observer object.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public <T extends Drop> void register(Class<T> dropType,
			DropObserver observer) {
		if (dropType == null)
			throw new IllegalArgumentException("dropType must be non-null");
		if (observer == null)
			throw new IllegalArgumentException("observer must be non-null");
		Set<DropObserver> observers = getObservers(dropType);
		observers.add(observer);
	}

	/**
	 * Removes an observer interested in status changes to drops of a specific
	 * type.
	 * <p>
	 * If the observer had not registered with this sea then this call has no
	 * effect.
	 * 
	 * @param dropType
	 *            the drop subtype of interest.
	 * @param observer
	 *            the concrete observer object.
	 * 
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null.
	 */
	public <T extends Drop> void unregister(Class<T> dropType,
			DropObserver observer) {
		if (dropType == null)
			throw new IllegalArgumentException("dropType must be non-null");
		if (observer == null)
			throw new IllegalArgumentException("observer must be non-null");
		Set<DropObserver> observers = getObservers(dropType);
		observers.remove(observer);
	}

	/**
	 * Gets the set of observers registered with this sea for a particular type
	 * of drops. The set is created if it doesn't exist.
	 * 
	 * @param dropType
	 *            the drop subtype of interest
	 * @return the set of observers for <code>dropType</code>.
	 */
	private <T extends Drop> Set<DropObserver> getObservers(Class<T> dropType) {
		Set<DropObserver> observers = f_dropTypeToObservers.get(dropType);
		if (observers == null) {
			observers = new HashSet<DropObserver>();
			f_dropTypeToObservers.put(dropType, observers);
		}
		return observers;
	}

	public void addSeaObserver(SeaObserver o) {
		f_seaObservers.add(o);
	}

	public void removeSeaObserver(SeaObserver o) {
		f_seaObservers.remove(o);
	}

	public void notifySeaObservers() {
		for (SeaObserver o : f_seaObservers) {
			o.seaChanged();
		}
	}

	/**
	 * Invalidates all drops contained within this sea. Acts as a
	 * straightforward reset method to invalidate all that is currently known.
	 */
	public void invalidateAll() {
		// we need to make a copy of the set of drops in the sea as the set will
		// be changing (rapidly) as we invalidate drops within it
		final Set<Drop> safeCopy = new HashSet<Drop>();
		safeCopy.addAll(f_validDrops);
		for (Drop drop : safeCopy) {
			drop.invalidate();
		}
		assert (f_validDrops.isEmpty());
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
		addMatchingDropsFrom(f_validDrops, pred, safeCopy);
		// safeCopy.addAll(validDropSet);
		for (Iterator<Drop> i = safeCopy.iterator(); i.hasNext();) {
			Drop drop = i.next();
			drop.invalidate();
		}
	}

	/**
	 * Causes the whole-program proof of model/code consistency to be run on
	 * this sea. Until this is done the ProofDrop "proof" methods do not contain
	 * valid information. Normally this method should be invoked after all
	 * analysis has been run and all results are reported into drop-sea.
	 * <p>
	 * This analysis is patterned after a flow analysis. It uses the following
	 * lattice:
	 * 
	 * <pre>
	 *        consistent
	 *     consistent/red dot
	 *    inconsistent/red dot
	 *       inconsistent
	 * </pre>
	 * 
	 * 
	 * @return a timestamp for when it's done
	 * @see ProofDrop#provedConsistent()
	 * @see ProofDrop#proofUsesRedDot()
	 */
	public synchronized long updateConsistencyProof() {
		if (timeStamp != INVALIDATED) {
			return timeStamp;
		}
		if (LOG.isLoggable(Level.FINE))
			LOG.fine("Updating consistency proof: " + timeStamp);

		/*
		 * Initialize drop-sea flow analysis "proof" (a drop-sea query)
		 */
		worklist = new LinkedList<ProofDrop>();
		Set<? extends ProofDrop> s = this.getDropsOfType(ProofDrop.class);
		for (ProofDrop d : s) {
			if (d instanceof PromiseDrop) {

				/*
				 * PROMISE DROP
				 */

				PromiseDrop pd = (PromiseDrop) d;

				// for a promise drop we flag a red dot if it is not checked by
				// analysis
				pd.proofUsesRedDot = !pd.isCheckedByAnalysis();
				if (pd.isAssumed())
					pd.proofUsesRedDot = true;

				// if no immediate result drops are an "X" then we are
				// consistent
				pd.provedConsistent = true; // assume true
				Set<? extends ResultDrop> analysisResults = pd.getCheckedBy();
				for (ResultDrop result : analysisResults) {
					/*
					 * & in local result
					 */
					pd.provedConsistent = pd.provedConsistent
							&& result.isConsistent();
				}
			} else if (d instanceof ResultDrop) {

				/*
				 * RESULT DROP
				 */

				ResultDrop rd = (ResultDrop) d;

				// result drops are, by definition, can not start off with a red
				// dot :-)
				rd.proofUsesRedDot = false;

				// record local result
				rd.provedConsistent = rd.isConsistent();

			} else {
				LOG
						.log(
								Level.SEVERE,
								"[Sea.updateConsistencyProof] SERIOUS ERROR - ProofDrop is not a PromiseDrop or a ResultDrop");
			}
			worklist.add(d);
		}

		/*
		 * Do "proof" until we reach a fixed-point (i.e., the worklist is empty)
		 */
		while (!worklist.isEmpty()) {
			Set<ProofDrop> nextWorklist = new HashSet<ProofDrop>(); // avoid
			// mutation during iteration
			for (ProofDrop d : worklist) {
				boolean oldProofIsConsistent = d.provedConsistent;
				boolean oldProofUsesRedDot = d.proofUsesRedDot;

				if (d instanceof PromiseDrop) {

					/*
					 * PROMISE DROP
					 */

					PromiseDrop pd = (PromiseDrop) d;

					// examine dependent analysis results and dependent promises
					Set<? extends ResultDrop> tpd = pd.getCheckedBy();

					Set<ProofDrop> proofDrops = new HashSet<ProofDrop>(tpd
							.size());
					for (ProofDrop t : tpd) {
						proofDrops.add(t);
					}
					proofDrops.addAll(Sea.filterDropsOfType(PromiseDrop.class,
							pd.getDependents()));
					for (ProofDrop result : proofDrops) {
						// all must be consistent for this drop to be consistent
						pd.provedConsistent &= result.provedConsistent;
						// any red dot means this drop depends upon a red dot
						if (result.proofUsesRedDot)
							pd.proofUsesRedDot = true;
					}
				} else if (d instanceof ResultDrop) {

					/*
					 * RESULT DROP
					 */

					ResultDrop rd = (ResultDrop) d;

					// "and" trust promise drops
					Set<PromiseDrop> andTrusts = rd.getTrusts();
					for (Iterator<PromiseDrop> j = andTrusts.iterator(); j
							.hasNext();) {
						PromiseDrop promise = j.next();
						// all must be consistent for this drop to be consistent
						rd.provedConsistent &= promise.provedConsistent;
						// any red dot means this drop depends upon a red dot
						if (promise.proofUsesRedDot)
							rd.proofUsesRedDot = true;
					}
					// "or" trust promise drops
					if (rd.hasOrLogic()) { // skip this in the common case
						boolean overall_or_Result = false;
						boolean overall_or_UsesRedDot = false;
						Set<String> orLabels = rd.get_or_TrustLabelSet();
						for (String orKey : orLabels) {
							boolean choiceResult = true;
							boolean choiceUsesRedDot = false;
							Set<? extends PromiseDrop> promiseSet = rd
									.get_or_Trusts(orKey);
							for (PromiseDrop promise : promiseSet) {
								// all must be consistent for this choice to be
								// consistent
								choiceResult &= promise.provedConsistent;
								// any red dot means this choice depends upon a
								// red dot
								if (promise.proofUsesRedDot)
									choiceUsesRedDot = true;
							}
							// should we choose this choice? Recall our lattice
							// is:
							// o consistent
							// o consistent/red dot
							// o inconsistent/red dot
							// o inconsistent
							// so we want to pick the "highest" result
							if (choiceResult) {
								if (!choiceUsesRedDot) {
									// best possible outcome
									overall_or_Result = choiceResult;
									overall_or_UsesRedDot = choiceUsesRedDot;
								} else {
									if (!overall_or_Result) {
										// take it, since so far we think we are
										// inconsistent
										overall_or_Result = choiceResult;
										overall_or_UsesRedDot = choiceUsesRedDot;
									}
								}
							} else {
								if (!choiceUsesRedDot) {
									if (!overall_or_Result) {
										// take it, since so far we might be
										// sure we are wrong
										overall_or_Result = choiceResult;
										overall_or_UsesRedDot = choiceUsesRedDot;
									}
								}
								// ignore bottom of lattice, this was our
								// default (set above)
							}
						}
						// add the choice selected into the overall result for
						// this drop
						// all must be consistent for this drop to be consistent
						rd.provedConsistent &= overall_or_Result;
						// any red dot means this drop depends upon a red dot
						if (overall_or_UsesRedDot)
							rd.proofUsesRedDot = true;
						// save in the drop
						rd.or_provedConsistent = overall_or_Result;
						rd.or_proofUsesRedDot = overall_or_UsesRedDot;
					}
				} else {
					LOG
							.log(
									Level.SEVERE,
									"[Sea.updateConsistencyProof] SERIOUS ERROR - ProofDrop is not a PromiseDrop or a ResultDrop");
				}

				// only add to worklist if something changed about the result
				boolean resultChanged = !(oldProofIsConsistent == d.provedConsistent && oldProofUsesRedDot == d.proofUsesRedDot);
				if (resultChanged) {
					nextWorklist.add(d);
					if (d instanceof PromiseDrop) {
						PromiseDrop pd = (PromiseDrop) d;
						// add all result drops trusted by this promise drop
						nextWorklist.addAll(pd.getTrustedBy());
						// add all deponent promise drops of this promise drop
						nextWorklist.addAll(Sea.filterDropsOfType(
								PromiseDrop.class, pd.getDeponents()));
					} else if (d instanceof ResultDrop) {
						ResultDrop rd = (ResultDrop) d;
						// add all promise drops that this result checks
						nextWorklist.addAll(rd.getChecks());
					}
				}
			}
			worklist.clear();
			worklist.addAll(nextWorklist);
		}

		timeStamp = System.currentTimeMillis();
		if (LOG.isLoggable(Level.FINE))
			LOG.fine("Done updating consistency proof: " + timeStamp);
		return timeStamp;
	}

	private List<ProofDrop> worklist;

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
		timeStamp = INVALIDATED;

		if (event == DropEvent.Created) {
			// add the new drop to this sea's list of valid drops
			f_validDrops.add(drop);
		} else if (event == DropEvent.Invalidated) {
			// remove the drop from this sea's list of valid drops
			f_validDrops.remove(drop);
		}
		// notify all registered observers of the status change
		Set<DropObserver> observers = new HashSet<DropObserver>();
		for (Class dropType : f_dropTypeToObservers.keySet()) {
			if (dropType.isInstance(drop)) {
				observers.addAll(f_dropTypeToObservers.get(dropType));
			}
		}
		for (DropObserver observer : observers) {
			observer.dropChanged(drop, event);
		}
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	/**
	 * The default sea instance.
	 */
	private static final Sea DEFAULT_SEA = new Sea();

	public static final long INVALIDATED = -1L;

	/**
	 * A timestamp of when the sea last updated the consistency proof
	 */
	private long timeStamp = INVALIDATED;

	/**
	 * The set of valid drops within this sea.
	 */
	private final Set<Drop> f_validDrops = new ConcurrentHashSet<Drop>(5000);

	/**
	 * A map from drop subtypes to a set of registered observers interested in
	 * status changes about the knowledge status of those drops.
	 */
	private final Map<Class, Set<DropObserver>> f_dropTypeToObservers = new ConcurrentHashMap<Class, Set<DropObserver>>();

	private final List<SeaObserver> f_seaObservers = new CopyOnWriteArrayList<SeaObserver>();
}