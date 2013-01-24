package com.surelogic.dropsea.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.ReturnsLock;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Represents a <i>sea</i> of knowledge, not intended to be subclassed. Sea
 * instances contain and manage <i>drops</i> of information. Instances form a
 * truth maintenance system by managing dependent and deponent drops.
 * <p>
 * The sea is intended to be shared between threads when analysis is run. Locks
 * are acquired on the object returned from {@link #getSeaLock()}. The code
 * locks internally, however, the lock can be acquired to perform a transaction
 * of many calls together.
 * 
 * @see Drop
 */
@Region("SeaState")
@RegionLock("SeaLock is f_seaLock protects SeaState")
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

  @Unique("return")
  public Sea() {
	  // Nothing to do
  }
  
  /**
   * Returns a new list that contains drops within <code>drops</code> that are
   * of <code>dropType</code> or any of its subtypes.
   * <p>
   * Typical use would be to subset a set of drops such that all drops in the
   * subset are assignment compatible with a specific type, as shown in the
   * below code snippet.
   * 
   * <pre>
   *    class MyDrop extends Drop { ... }
   *    class MySubDrop extends MyDrop { ... }
   *    MyDrop d1 = new MyDrop();
   *    MySubDrop d2 = new MySubDrop();
   *      
   *    List&lt;Drop&gt; r = Sea.getDefault().getDrops();
   *    (NOTE) r = { d1, d2 }
   *    List&lt;MyDrop&gt; r1 = Sea.filterDropsOfType(MyDrop.class, r);
   *    (NOTE) r1 = { d1, d2 }
   *    List&lt;MySubDrop&gt; r2 = Sea.filterDropsOfType(MySubDrop.class, r);
   *    (NOTE) r2 = { d2 }
   * </pre>
   * 
   * @param dropType
   *          the type of drops desired.
   * @param drops
   *          the collection of drops to subset. This collection is not
   *          modified.
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop> ArrayList<T> filterDropsOfType(Class<T> dropType, Collection<? extends Drop> drops) {
    if (dropType == null)
      throw new IllegalArgumentException(I18N.err(44, "dropType"));
    if (drops == null)
      throw new IllegalArgumentException(I18N.err(44, "drops"));

    final ArrayList<T> result = new ArrayList<T>();
    for (final Drop drop : drops) {
      if (dropType.isInstance(drop)) {
        @SuppressWarnings("unchecked")
        final T dropToAdd = (T) drop;
        result.add(dropToAdd);
      }
    }
    return result;
  }

  /**
   * Returns a new list that contains drops within <code>drops</code> that are
   * of <code>dropType</code>&mdash;subtypes are <i>not</i> included.
   * <p>
   * Typical use would be to subset a set of drops such that all drops in the
   * subset are of a specific type, as shown in the below code snippet.
   * 
   * <pre>
   *    class MyDrop extends Drop { ... }
   *    class MySubDrop extends MyDrop { ... }
   *    MyDrop d1 = new MyDrop();
   *    MySubDrop d2 = new MySubDrop();
   *      
   *    List&lt;Drop&gt; r = Sea.getDefault().getDrops();
   *    (NOTE) r = { d1, d2 }
   *    List&lt;MyDrop&gt; r1 = Sea.filterDropsOfExactType(MyDrop.class, r);
   *    (NOTE) r1 = { d1 }
   *    List&lt;MySubDrop&gt; r2 = Sea.filterDropsOfExactType(MySubDrop.class, r);
   *    (NOTE) r2 = { d2 }
   * </pre>
   * 
   * @param dropType
   *          the exact type of drops desired.
   * @param drops
   *          the collection of drops to subset. This collection is not
   *          modified.
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop> ArrayList<T> filterDropsOfExactType(Class<T> dropType, Collection<? extends Drop> drops) {
    if (dropType == null)
      throw new IllegalArgumentException(I18N.err(44, "dropType"));
    if (drops == null)
      throw new IllegalArgumentException(I18N.err(44, "drops"));

    final ArrayList<T> result = new ArrayList<T>();
    for (final Drop drop : drops) {
      if (drop.getClass().equals(dropType)) {
        @SuppressWarnings("unchecked")
        final T dropToAdd = (T) drop;
        result.add(dropToAdd);
      }
    }
    return result;
  }

  /**
   * Mutates the <code>mutableDrops</code> collection removing all drops from it
   * that are not of <code>dropType</code> or any of its subtypes. This method
   * returns a reference to <code>mutableDrops</code>.
   * 
   * <pre>
   *    class MyDrop extends Drop { ... }
   *    class MySubDrop extends MyDrop { ... }
   *    MyDrop d1 = new MyDrop();
   *    MySubDrop d2 = new MySubDrop();
   *      
   *    List&lt;Drop&gt; r = Sea.getDefault().getDrops();
   *    (NOTE) r = { d1, d2 }
   *    List&lt;Drop&gt; r1 = Sea.filterDropsOfTypeMutate(MySubDrop.class, r);
   *    (NOTE) r1 = { d2 }
   *    (NOTE) r1.equals(r)
   * </pre>
   * 
   * @param dropType
   *          the type of drops desired.
   * @param mutableDrops
   *          the collection of drops to mutate.
   * @return a reference to <tt>mutableDrops</tt>.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop, C extends Collection<Drop>> C filterDropsOfTypeMutate(Class<T> dropType, C mutableDrops) {
    if (dropType == null)
      throw new IllegalArgumentException(I18N.err(44, "dropType"));
    if (mutableDrops == null)
      throw new IllegalArgumentException(I18N.err(44, "mutableDrops"));

    for (final Iterator<Drop> i = mutableDrops.iterator(); i.hasNext();) {
      final Drop drop = i.next();
      if (!dropType.isInstance(drop)) {
        i.remove();
      }
    }
    return mutableDrops;
  }

  /**
   * Mutates the <code>mutableDrops</code> collection removing all drops from it
   * that are not of <code>dropType</code>&mdash;subtypes are removed. This
   * method returns a reference to <code>mutableDrops</code>.
   * 
   * <pre>
   *    class MyDrop extends Drop { ... }
   *    class MySubDrop extends MyDrop { ... }
   *    MyDrop d1 = new MyDrop();
   *    MySubDrop d2 = new MySubDrop();
   *      
   *    List&lt;Drop&gt; r = Sea.getDefault().getDrops();
   *    (NOTE) r = { d1, d2 }
   *    List&lt;Drop&gt; r1 = Sea.filterDropsOfExactTypeMutate(MyDrop.class, r);
   *    (NOTE) r1 = { d1 }
   *    (NOTE) r1.equals(r)
   * </pre>
   * 
   * @param dropType
   *          the exact type of drops desired.
   * @param mutableDrops
   *          the collection of drops to mutate.
   * @return a reference to <tt>mutableDrops</tt>.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop, C extends Collection<Drop>> C filterDropsOfExactTypeMutate(Class<T> dropType, C mutableDrops) {
    if (dropType == null)
      throw new IllegalArgumentException(I18N.err(44, "dropType"));
    if (mutableDrops == null)
      throw new IllegalArgumentException(I18N.err(44, "mutableDrops"));

    for (final Iterator<Drop> i = mutableDrops.iterator(); i.hasNext();) {
      final Drop drop = i.next();
      if (!drop.getClass().equals(dropType)) {
        i.remove();
      }
    }
    return mutableDrops;
  }

  /**
   * Returns a new list of drops within <code>drops</code> that match the given
   * drop predicate.
   * 
   * @param pred
   *          a drop predicate.
   * @param drops
   *          the collection of drops to subset. This collection is not
   *          modified.
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop> ArrayList<T> filterDropsMatching(DropPredicate pred, Collection<T> drops) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (drops == null)
      throw new IllegalArgumentException(I18N.err(44, "drops"));

    final ArrayList<T> result = new ArrayList<T>(drops);
    filterDropsMatchingMutate(pred, result);
    return result;
  }

  /**
   * Mutates the given drop set by removing all drops within it that do not
   * match the given drop predicate. This method returns a reference to
   * <code>mutableDrops</code>.
   * 
   * @param pred
   *          a drop predicate.
   * @param mutableDrops
   *          the set of drops to mutate.
   * @return a reference to <tt>mutableDrops</tt>.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop, C extends Collection<T>> C filterDropsMatchingMutate(DropPredicate pred, C mutableDrops) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (mutableDrops == null)
      throw new IllegalArgumentException(I18N.err(44, "mutableDrops"));

    for (final Iterator<T> i = mutableDrops.iterator(); i.hasNext();) {
      final T drop = i.next();
      if (!pred.match(drop)) {
        i.remove();
      }
    }
    return mutableDrops;
  }

  /**
   * Queries if at least one drop in <tt>drops</tt> is matched by a drop
   * predicate.
   * 
   * @param pred
   *          the drop predicate to use.
   * @param drops
   *          the set of drops to examine.
   * @return <code>true</code> if at least one drop matches, <code>false</code>
   *         otherwise.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static boolean hasMatchingDrops(DropPredicate pred, Collection<? extends Drop> drops) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (drops == null)
      throw new IllegalArgumentException(I18N.err(44, "drops"));

    for (final Drop drop : drops) {
      if (pred.match(drop)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets a new list of all the valid drops in this sea.
   * 
   * @return a list of drops.
   */
  public ArrayList<Drop> getDrops() {
    synchronized (f_seaLock) {
      return new ArrayList<Drop>(f_validDrops);
    }
  }

  /**
   * Returns a new list of drops within this sea that are of
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
   *    Sea.getDefault().getDropsOfType(Drop.class) = { d1, d2, d3 }
   *    Sea.getDefault().getDropsOfType(MyDrop.class) = { d2, d3 }
   *    Sea.getDefault().getDropsOfType(MySubDrop.class) = { d3 }
   * </pre>
   * 
   * @param dropType
   *          the type of drops desired.
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public <T extends Drop> List<T> getDropsOfType(Class<T> dropType) {
    synchronized (f_seaLock) {
      return filterDropsOfType(dropType, f_validDrops);
    }
  }

  /**
   * Returns a new list of drops within this sea that are of
   * <code>dropType</code>&mdash;subtypes are <i>not</i> included.
   * <p>
   * Typical use would be to extract all drops in the sea that are of a specific
   * type, as shown in the below code snippet.
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
   *          the type of drops desired.
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public <T extends Drop> List<T> getDropsOfExactType(Class<T> dropType) {
    synchronized (f_seaLock) {
      return filterDropsOfExactType(dropType, f_validDrops);
    }
  }

  /**
   * Returns a new list of drops that is matched by a drop predicate.
   * 
   * @param pred
   *          a drop predicate
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public List<Drop> getDropsMatching(DropPredicate pred) {
    synchronized (f_seaLock) {
      return filterDropsMatching(pred, f_validDrops);
    }
  }

  /**
   * Invalidates all drops contained within this sea. Acts as a straightforward
   * reset method to invalidate all that is currently known.
   */
  public void invalidateAll() {
    // we need to make a copy of the set of drops in the sea as the set will
    // be changing (rapidly) as we invalidate drops within it
    final Collection<Drop> safeCopy;
    synchronized (f_seaLock) {
      safeCopy = new ArrayList<Drop>(f_validDrops);
    }
    for (Drop drop : safeCopy) {
      drop.invalidate();
    }
  }

  /**
   * Invalidates all drops contained within this sea that match the passed drop
   * predicate.
   * 
   * @param pred
   *          The predicate to match drops against.
   */
  public void invalidateMatching(DropPredicate pred) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));

    /*
     * we need to make a copy of the set of drops in the sea as the set will be
     * changing (rapidly) as we invalidate drops within it
     */
    final Collection<Drop> safeCopy;
    synchronized (f_seaLock) {
      safeCopy = Sea.filterDropsMatching(pred, f_validDrops);
    }
    for (final Drop drop : safeCopy) {
      drop.invalidate();
    }
  }

  /**
   * Causes the whole-program proof of model/code consistency to be run on this
   * sea. Until this is done the ProofDrop "proof" methods do not contain valid
   * information. Normally this method should be invoked after all analysis has
   * been run and all results are reported into drop-sea.
   * <p>
   * This analysis is patterned after a reverse flow analysis. It uses the
   * following lattice:
   * 
   * <pre>
   *        consistent
   *     consistent/red dot
   *    inconsistent/red dot
   *       inconsistent
   * </pre>
   * 
   * The methods {@link ProofDrop#proofInitialize()} and
   * {@link ProofDrop#proofTransfer()} are invoked by this algorithm.
   * 
   * @return a timestamp for when it's done
   */
  public long updateConsistencyProof() {
    synchronized (f_seaLock) {
      if (f_timeStamp != INVALIDATED) {
        return f_timeStamp;
      }

      // run hooks (if any)
      for (SeaConsistencyProofHook hook : f_proofHooks)
        hook.preConsistencyProof(this);

      // get all the proof drops
      final List<ProofDrop> allProofDrops = getDropsOfType(ProofDrop.class);

      /*
       * INITIALIZE drop-sea flow analysis "proof"
       */

      for (ProofDrop d : allProofDrops) {
        d.proofInitialize();
      }

      /*
       * ITERATE until we reach a FIXED-POINT (i.e., no changes)
       */

      // consistency, red-dot, derived-from-src, derived-from-warning
      boolean changed = true;
      while (changed) {
        changed = false;
        for (ProofDrop d : allProofDrops) {
          // transfer from "lower" drops
          changed |= d.proofTransfer();
        }
      }

      // used by proof
      changed = true;
      while (changed) {
        changed = false;
        for (ProofDrop d : allProofDrops) {
          if (d instanceof AnalysisResultDrop) {
            // transfer from "higher" drops
            changed |= ((AnalysisResultDrop) d).proofTransferUsedBy();
          }
        }
      }

      /*
       * FINALIZE drop-sea flow analysis "proof"
       */
      for (ProofDrop d : allProofDrops)
        d.proofFinalize();

      // run hooks (if any)
      for (SeaConsistencyProofHook hook : f_proofHooks)
        hook.postConsistencyProof(this);

      f_timeStamp = System.currentTimeMillis();
      if (LOG.isLoggable(Level.FINE))
        LOG.fine("Done updating consistency proof: " + f_timeStamp);
      return f_timeStamp;
    }
  }

  /**
   * Notification to this sea that something about the knowledge status of a
   * drop has changed. This method should only be invoked from the {@link Drop}
   * class. This method orchestrates change notifications back to clients code
   * listeners.
   * 
   * @param drop
   *          the drop the notification is about.
   * @param event
   *          what happened to the drop.
   */
  void notify(Drop drop, DropEvent event) {
    synchronized (f_seaLock) {
      f_timeStamp = INVALIDATED;

      if (event == DropEvent.Created) {
        // add the new drop to this sea's list of valid drops
        f_validDrops.add(drop);
      } else if (event == DropEvent.Invalidated) {
        // remove the drop from this sea's list of valid drops
        f_validDrops.remove(drop);
      }
    }
  }

  public long getTimeStamp() {
    synchronized (f_seaLock) {
      return f_timeStamp;
    }
  }

  /**
   * The default sea instance.
   */
  private static final Sea DEFAULT_SEA = new Sea();

  public static final long INVALIDATED = -1L;

  /**
   * A timestamp of when the sea last updated the consistency proof
   */
  @InRegion("SeaState")
  private long f_timeStamp = INVALIDATED;

  /**
   * Adds code to run before and/or after the consistency proof is run on every
   * call to {@link Sea#updateConsistencyProof()}.
   * 
   * @param hook
   *          an consistency proof hook instance.
   */
  public void addConsistencyProofHook(SeaConsistencyProofHook hook) {
    f_proofHooks.add(hook);
  }

  /**
   * Removes code to run before and/or after the consistency proof is run on
   * every call to {@link Sea#updateConsistencyProof()}.
   * 
   * @param hook
   *          an consistency proof hook instance.
   */
  public void removeConsistencyProofHook(SeaConsistencyProofHook hook) {
    f_proofHooks.remove(hook);
  }

  // Made anonymous to customize the type used for the lock
  private final Object f_seaLock = new Object() {};

  /**
   * Gets the lock for this sea.
   * 
   * @return the non-null lock for this sea.
   */
  @ReturnsLock("SeaLock")
  public final Object getSeaLock() {
    return f_seaLock;
  }

  /**
   * The set of valid drops within this sea.
   */
  @UniqueInRegion("SeaState")
  private final List<Drop> f_validDrops = new ArrayList<Drop>(5000);

  /**
   * The set of consistency proof hooks.
   */
  private final CopyOnWriteArraySet<SeaConsistencyProofHook> f_proofHooks = new CopyOnWriteArraySet<SeaConsistencyProofHook>();
}
