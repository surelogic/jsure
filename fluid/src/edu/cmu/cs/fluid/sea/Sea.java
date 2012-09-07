package edu.cmu.cs.fluid.sea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.i18n.I18N;
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
  public static <T extends Drop> List<T> filterDropsOfType(Class<T> dropType, Collection<Drop> drops) {
    if (dropType == null)
      throw new IllegalArgumentException(I18N.err(44, "dropType"));
    if (drops == null)
      throw new IllegalArgumentException(I18N.err(44, "drops"));

    final List<T> result = new ArrayList<T>();
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
  public static <T extends Drop> List<T> filterDropsOfExactType(Class<T> dropType, Collection<Drop> drops) {
    if (dropType == null)
      throw new IllegalArgumentException(I18N.err(44, "dropType"));
    if (drops == null)
      throw new IllegalArgumentException(I18N.err(44, "drops"));

    final List<T> result = new ArrayList<T>();
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
   * Returns a new set that contains drops within <code>dropSet</code> that
   * match the given drop predicate.
   * 
   * @param pred
   *          the drop predicate to apply to the drop set.
   * @param dropSet
   *          the set of drops to subset. This set is not modified.
   * @return the set of drops in <code>dropSet</code> that matched the drop
   *         predicate.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop> Set<T> filter(DropPredicate pred, Collection<T> dropSet) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (dropSet == null)
      throw new IllegalArgumentException(I18N.err(44, "dropSet"));

    final Set<T> result = new HashSet<T>(dropSet);
    filterMutate(pred, result);
    return result;
  }

  /**
   * Mutates the given drop set by removing all drops within it that do not
   * match the given drop predicate.
   * 
   * @param pred
   *          the drop predicate to apply to the drop set.
   * @param mutableDropSet
   *          the set of drops to mutate.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends Drop> void filterMutate(DropPredicate pred, Collection<T> mutableDropSet) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (mutableDropSet == null)
      throw new IllegalArgumentException(I18N.err(44, "mutableDropSet"));

    for (Iterator<T> i = mutableDropSet.iterator(); i.hasNext();) {
      Drop drop = i.next();
      if (!pred.match(drop)) {
        i.remove();
      }
    }
  }

  /**
   * Queries if at least one drop in the given set is matched by the given drop
   * predicate.
   * 
   * @param pred
   *          the drop predicate to use.
   * @param dropSet
   *          the drop set to examine.
   * @return <code>true</code> if at least one drop matches, <code>false</code>
   *         otherwise.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static boolean hasMatchingDrops(DropPredicate pred, Collection<? extends Drop> dropSet) {
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (dropSet == null)
      throw new IllegalArgumentException(I18N.err(44, "dropSet"));

    for (Drop drop : dropSet) {
      if (pred.match(drop)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds references to those drops in the source set that match the given drop
   * predicate into the result set.
   * 
   * @param sourceDropSet
   *          the source drop set. This set is not modified.
   * @param pred
   *          the drop predicate to apply to the source drop set.
   * @param mutableResultDropSet
   *          the result set to add matching drops into.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public static <T extends IDrop> void addMatchingDropsFrom(Collection<? extends T> sourceDropSet, DropPredicate pred,
      Collection<T> mutableResultDropSet) {
    if (sourceDropSet == null)
      throw new IllegalArgumentException(I18N.err(44, "sourceDropSet"));
    if (pred == null)
      throw new IllegalArgumentException(I18N.err(44, "pred"));
    if (mutableResultDropSet == null)
      throw new IllegalArgumentException(I18N.err(44, "mutableResultDropSet"));

    for (T drop : sourceDropSet) {
      if (pred.match(drop)) {
        mutableResultDropSet.add(drop);
      }
    }
  }

  /**
   * Gets the annotation name for the passed promise drop information. Returns
   * {@code null} if the drop information passed is not about a promise drop or
   * the annotation name cannot be determined.
   * <p>
   * <i>Implementation Note:</i> This uses the type name so that
   * <tt>StartsPromiseDrop</tt> would return <tt>Starts</tt>.
   * 
   * @param promiseDropInfo
   *          the promise drop information.
   * @return the annotation name or {@code null}.
   */
  public static String getAnnotationName(IProofDrop promiseDropInfo) {
    final String suffix = "PromiseDrop";
    if (!promiseDropInfo.instanceOf(PromiseDrop.class))
      return null;
    final String result = promiseDropInfo.getTypeName();
    if (result == null)
      return null;
    // Special cases
    if ("LockModel".equals(result))
      return "RegionLock";
    if ("RegionModel".equals(result))
      return "Region";
    if ("VouchFieldIsPromiseDrop".equals(result))
      return "Vouch";
    // General case XResultDrop where we return X
    if (!result.endsWith(suffix))
      return null;
    return result.substring(0, result.length() - suffix.length());
  }

  /**
   * Gets a new list of all the valid drops in this sea.
   * 
   * @return a list of drops.
   */
  public List<Drop> getDrops() {
    synchronized (f_validDrops) {
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
    synchronized (f_validDrops) {
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
    synchronized (f_validDrops) {
      return filterDropsOfExactType(dropType, f_validDrops);
    }
  }

  /**
   * Returns a new list of drops that is matched by <tt>pred</tt>.
   * 
   * @param pred
   * @return a list of matching drops.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public List<Drop> getDropsMatching(DropPredicate pred) {
    synchronized (f_validDrops) {
      return null; // TODO
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
   *          the drop subtype of interest.
   * @param observer
   *          the concrete observer object.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public <T extends Drop> void register(Class<T> dropType, DropObserver observer) {
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
   *          the drop subtype of interest.
   * @param observer
   *          the concrete observer object.
   * 
   * @throws IllegalArgumentException
   *           if any of the parameters are null.
   */
  public <T extends Drop> void unregister(Class<T> dropType, DropObserver observer) {
    if (dropType == null)
      throw new IllegalArgumentException("dropType must be non-null");
    if (observer == null)
      throw new IllegalArgumentException("observer must be non-null");
    Set<DropObserver> observers = getObservers(dropType);
    observers.remove(observer);
  }

  /**
   * Gets the set of observers registered with this sea for a particular type of
   * drops. The set is created if it doesn't exist.
   * 
   * @param dropType
   *          the drop subtype of interest
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
   * Invalidates all drops contained within this sea. Acts as a straightforward
   * reset method to invalidate all that is currently known.
   */
  public void invalidateAll() {
    // we need to make a copy of the set of drops in the sea as the set will
    // be changing (rapidly) as we invalidate drops within it
    final Collection<Drop> safeCopy;
    synchronized (f_validDrops) {
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
      return;

    /*
     * we need to make a copy of the set of drops in the sea as the set will be
     * changing (rapidly) as we invalidate drops within it
     */
    final Set<Drop> safeCopy = new HashSet<Drop>();
    addMatchingDropsFrom(f_validDrops, pred, safeCopy);
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
    if (f_timeStamp != INVALIDATED) {
      return f_timeStamp;
    }

    // TODO BAD TO DO THIS HOLDING LOCK
    for (SeaConsistencyProofHook hook : f_proofHooks)
      hook.preConsistencyProof(this);

    /*
     * Initialize drop-sea flow analysis "proof" (a drop-sea query)
     */
    final List<ProofDrop> worklist = new ArrayList<ProofDrop>();
    final List<ProofDrop> s = this.getDropsOfType(ProofDrop.class);
    for (ProofDrop d : s) {
      if (d instanceof PromiseDrop) {

        /*
         * PROMISE DROP
         */

        @SuppressWarnings("unchecked")
        final PromiseDrop<? extends IAASTRootNode> pd = (PromiseDrop<? extends IAASTRootNode>) d;

        // for a promise drop we flag a red dot if it is not checked by
        // analysis
        pd.proofUsesRedDot = !pd.isCheckedByAnalysis();
        if (pd.isAssumed())
          pd.proofUsesRedDot = true;

        // if no immediate result drops are an "X" then we are
        // consistent
        pd.provedConsistent = true; // assume true
        pd.derivedFromSrc = pd.isFromSrc();

        Collection<ResultDrop> analysisResults = pd.getCheckedBy();
        for (ResultDrop result : analysisResults) {
          /*
           * & in local result
           */
          pd.provedConsistent = pd.provedConsistent && (result.isConsistent() || result.isVouched());

          pd.derivedFromSrc = pd.derivedFromSrc || result.isFromSrc();
        }
      } else if (d instanceof ResultDrop) {

        /*
         * RESULT DROP
         */

        ResultDrop rd = (ResultDrop) d;

        // result drops, by definition, can not start off with a red dot
        rd.proofUsesRedDot = false;

        // record local result
        rd.provedConsistent = rd.isConsistent() || rd.isVouched();

        rd.derivedFromSrc = rd.isFromSrc();
      } else if (d instanceof ResultFolderDrop) {

          /*
           * RESULT FOLDER DROP
           */

    	  ResultFolderDrop rd = (ResultFolderDrop) d;

          // result drops, by definition, can not start off with a red dot
          rd.proofUsesRedDot = false;

          rd.provedConsistent = true;

          rd.derivedFromSrc = rd.isFromSrc();
      } else {
        LOG.log(Level.SEVERE, "[Sea.updateConsistencyProof] SERIOUS ERROR - ProofDrop is not a PromiseDrop or a ResultDrop");
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
        boolean oldDerivedFromSrc = d.derivedFromSrc;

        if (d instanceof PromiseDrop) {

          /*
           * PROMISE DROP
           */

          @SuppressWarnings("unchecked")
          final PromiseDrop<? extends IAASTRootNode> pd = (PromiseDrop<? extends IAASTRootNode>) d;

          // examine dependent analysis results and dependent promises
          final Set<ProofDrop> proofDrops = new HashSet<ProofDrop>();
          proofDrops.addAll(pd.getCheckedBy());
          proofDrops.addAll(Sea.filterDropsOfType(PromiseDrop.class, pd.getDependents()));
          for (ProofDrop result : proofDrops) {
            // all must be consistent for this promise to be consistent
            pd.provedConsistent &= result.provedConsistent;
            // any red dot means this promise depends upon a red dot
            if (result.proofUsesRedDot)
              pd.proofUsesRedDot = true;
            // push along if derived from source code
            pd.derivedFromSrc |= result.derivedFromSrc;
          }
        } else if (d instanceof ResultFolderDrop) {

          /*
           * RESULT FOLDER DROP
           */

          final ResultFolderDrop dfd = (ResultFolderDrop) d;
          for (AbstractResultDrop result : dfd.getContents()) {
            // all must be consistent for this folder to be consistent
            dfd.provedConsistent &= result.provedConsistent;
            // any red dot means this folder depends upon a red dot
            if (result.proofUsesRedDot)
              dfd.proofUsesRedDot = true;
            // push along if derived from source code
            dfd.derivedFromSrc |= result.derivedFromSrc;
          }
        } else if (d instanceof ResultDrop) {

          /*
           * RESULT DROP
           */

          final ResultDrop rd = (ResultDrop) d;

          // "and" trust promise drops
          Set<PromiseDrop<? extends IAASTRootNode>> andTrusts = rd.getTrusts();
          for (final PromiseDrop<? extends IAASTRootNode> promise : andTrusts) {
            // all must be consistent for this drop to be consistent
            rd.provedConsistent &= promise.provedConsistent;
            // any red dot means this drop depends upon a red dot
            if (promise.proofUsesRedDot)
              rd.proofUsesRedDot = true;
            // if anything is derived from source we will be as well
            rd.derivedFromSrc |= promise.derivedFromSrc;
          }

          // "or" trust promise drops
          if (rd.hasOrLogic()) { // skip this in the common case
            boolean overall_or_Result = false;
            boolean overall_or_UsesRedDot = false;
            boolean overall_or_derivedFromSource = false;
            Set<String> orLabels = rd.get_or_TrustLabelSet();
            for (String orKey : orLabels) {
              boolean choiceResult = true;
              boolean choiceUsesRedDot = false;
              Set<? extends PromiseDrop<? extends IAASTRootNode>> promiseSet = rd.get_or_Trusts(orKey);
              for (PromiseDrop<? extends IAASTRootNode> promise : promiseSet) {
                // all must be consistent for this choice to be consistent
                choiceResult &= promise.provedConsistent;
                // any red dot means this choice depends upon a red dot
                if (promise.proofUsesRedDot)
                  choiceUsesRedDot = true;
                // if anything is derived from source we will be as well
                overall_or_derivedFromSource |= promise.derivedFromSrc;
              }
              // should we choose this choice? Our lattice is:
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
                    // take it, since so far we think we are inconsistent
                    overall_or_Result = choiceResult;
                    overall_or_UsesRedDot = choiceUsesRedDot;
                  }
                }
              } else {
                if (!choiceUsesRedDot) {
                  if (!overall_or_Result) {
                    // take it, since so far we might be sure we are wrong
                    overall_or_Result = choiceResult;
                    overall_or_UsesRedDot = choiceUsesRedDot;
                  }
                }
                // ignore bottom of lattice, this was our default (set above)
              }
            }
            /*
             * add the choice selected into the overall result for this drop all
             * must be consistent for this drop to be consistent
             */
            rd.provedConsistent &= overall_or_Result;
            /*
             * any red dot means this drop depends upon a red dot
             */
            if (overall_or_UsesRedDot)
              rd.proofUsesRedDot = true;
            /*
             * save in the drop
             */
            rd.or_provedConsistent = overall_or_Result;
            rd.or_proofUsesRedDot = overall_or_UsesRedDot;
            rd.derivedFromSrc |= overall_or_derivedFromSource;
          }
        } else {
          final String msg = I18N.err(246);
          LOG.log(Level.SEVERE, msg, new IllegalStateException(msg));
        }

        /*
         * only add to worklist if something changed about the result
         */
        boolean resultChanged = !(oldProofIsConsistent == d.provedConsistent && oldProofUsesRedDot == d.proofUsesRedDot && oldDerivedFromSrc == d.derivedFromSrc);
        if (resultChanged) {
          nextWorklist.add(d);
          if (d instanceof PromiseDrop) {
            @SuppressWarnings("unchecked")
            PromiseDrop<? extends IAASTRootNode> pd = (PromiseDrop<? extends IAASTRootNode>) d;
            // add all result drops trusted by this promise drop
            nextWorklist.addAll(pd.getTrustedBy());
            // add all deponent promise drops of this promise drop
            nextWorklist.addAll(Sea.filterDropsOfType(PromiseDrop.class, pd.getDeponents()));
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

    f_timeStamp = System.currentTimeMillis();
    if (LOG.isLoggable(Level.FINE))
      LOG.fine("Done updating consistency proof: " + f_timeStamp);

    for (SeaConsistencyProofHook hook : f_proofHooks)
      hook.postConsistencyProof(this);

    return f_timeStamp;
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
    f_timeStamp = INVALIDATED;

    if (event == DropEvent.Created) {
      // add the new drop to this sea's list of valid drops
      f_validDrops.add(drop);
    } else if (event == DropEvent.Invalidated) {
      // remove the drop from this sea's list of valid drops
      f_validDrops.remove(drop);
    }
    // notify all registered observers of the status change
    Set<DropObserver> observers = new HashSet<DropObserver>();
    for (Class<?> dropType : f_dropTypeToObservers.keySet()) {
      if (dropType.isInstance(drop)) {
        observers.addAll(f_dropTypeToObservers.get(dropType));
      }
    }
    for (DropObserver observer : observers) {
      observer.dropChanged(drop, event);
    }
  }

  public long getTimeStamp() {
    return f_timeStamp;
  }

  /**
   * The default sea instance.
   */
  private static final Sea DEFAULT_SEA = new Sea();

  public static final long INVALIDATED = -1L;

  /**
   * A timestamp of when the sea last updated the consistency proof
   */
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

  /**
   * The set of valid drops within this sea.
   */
  private final List<Drop> f_validDrops = new ArrayList<Drop>(5000);

  /**
   * A map from drop subtypes to a set of registered observers interested in
   * status changes about the knowledge status of those drops.
   */
  private final Map<Class<?>, Set<DropObserver>> f_dropTypeToObservers = new ConcurrentHashMap<Class<?>, Set<DropObserver>>();

  private final CopyOnWriteArrayList<SeaObserver> f_seaObservers = new CopyOnWriteArrayList<SeaObserver>();

  private final CopyOnWriteArrayList<SeaConsistencyProofHook> f_proofHooks = new CopyOnWriteArrayList<SeaConsistencyProofHook>();
}