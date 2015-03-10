/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedDerivedInformation.java,v 1.21 2008/09/04 15:20:48 chance Exp $
 * Created on Jun 3, 2004
 */
package edu.cmu.cs.fluid.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.derived.IDerivedInformation;
import edu.cmu.cs.fluid.util.ThreadGlobal;


/**
 * A container for information that depends on the current version.
 * This class keeps track of which versions the information is
 * available for.  The concrete subclass maintains the information.
 * The information is considered computed for all versions in a contiguous
 * subgraph of the version tree.  This continguous subtree is defined by
 * a root and a fringe set, both of which change as more information
 * is made available.
 * <p>
 * There are two main ways to use this abstract class: 
 * <ol>
 * <li> Subclasses
 * can make available ways to derive the information on demand, which are
 * then called as needed.  See {@link #deriveInitial}, {@link #deriveParent}
 * and {@link #deriveChild} where each is called for every new version to add
 * to the subtree of available versions.
 * <li> Subclasses can eagerly produce information and inform this class
 * that the information is available using {@link #assumeDerived}.
 * </ol>
 * It is permitted to use both approaches, but usually easier to do one or the other.
 * <p>
 * This class also permits careful segregation of mutation of information;
 * optionally (for subclasses), it can check whether a mutation is done
 * only in the process of deriving information.
 * @author boyland
 * @see VersionedDerivedMap
 */
public abstract class VersionedDerivedInformation implements IDerivedInformation {


  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version.derive");

  /**
   * The root of the subgraph of the version tree for which
   * information is derived/available.  If this is null, then
   * no information is yet available.
   */
  private Version rootVersion;
  
  /**
   * We map any dynasty founder to the last version in the dynasty
   * that is in the computed region.
   */
  private Map<Version,Version> dynastyDescendants = new HashMap<Version,Version>();
  
  /**
   * Instances of this class indicate whether we are current in the process of performing
   * a derivation to one or more new versions.  While in the process of
   * deriving these versions, we assume that the information is correct for these versions.
   * Instances of this class are always accessed only by the thread in which they were created.
   * @author boyland
   */
  private interface Derivation {
    /**
     * Return the first version for the derivation.  This version is
     * the ``marker version'' and is used to prevent other threads
     * from attempting to perform the same derivation at the same time.
     * @return first new version being derived.
     */
    public Version firstVersion();
    
    /**
     * Perform the derivation.
     * Performing the derivation is accomplished through calls
     * to {@link #deriveParent} and {@link #deriveChild}.
     */
    public void process();
    
    /**
     * Record the completion of this derivation using calls to 
     * {@link #assumeDerived}.
     */
    public void recordCompletion();
    
    /**
     * Return true, if derivation of information for this version is done,
     * or is currently in process.
     * @param v
     * @return
     */
    public boolean isInProcess(Version v);
  }

  /**
   * Non-null pair of versions for a thread which is in the process of deriving 
   * information for new versions.  The structure may only be mutated for versions
   * between the bounds (inclusive).  Only one derivation may be going on at a time.
   */
  private ThreadGlobal<Derivation> deriving = new ThreadGlobal<Derivation>(null);
  
  /**
   * Set of versions just off the fringe where derivation is going on.
   * This is used to ensure we don't start deriving for multiple versions at once.
   */
  private Set<Version> derivingVersions = new HashSet<Version>();
  
  /**
   * Create a new set of information which is not currently derived for
   * any version.
   */
  protected VersionedDerivedInformation() {
	  // Nothing to do
  }
  
  /** Start a new set of information in which the information
   * at this version is already derived, or will be derived once
   * the constructors are all done.
   * @param root version at which information is initialized
   *   (or null in which case, we wait for the first demand.)
   */
  @Unique("return")
  protected VersionedDerivedInformation(Version root) {
    if (root != null) setRootVersion(root);
  }

  /**
   * Discard all knowledge of derivations performed.
   * This method waits until all threads stop performing derivations.
   * In the worst case, this method will ``starve.'' Since this method
   * is not actually useful unless people aren't deriving information frequently,
   * then there is no urgency to fix this starvation problem.
   * @throws DerivationException if this thread is currently deriving
   */
  @Override
  public synchronized void clear() {
    if (isDeriving()) {
      throw new DerivationException("currently dderiving in this thread: cannot clear");
    }
    while (!derivingVersions.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    rootVersion = null;
    dynastyDescendants.clear();
  }
  
  /**
   * Set the root version (and its entry in the descendants map)
   * We need to have the lock or the instance is still thread-local.
   * @param root
   */
  @Borrowed("this")
  private void setRootVersion(Version root) {
    rootVersion = root;
    dynastyDescendants.put(root.getDynastyFounder(),root);
  }
  
  /**
   * Set the internal state so that the given version is marked as derived.
   * It also marks all versions between this one and our "root" as derived too,
   * which can be unexpected especially if multiple threads as calling this code.
   * In this case, the code can also be inefficient.
   * @param v version that should be assumed derived.
   */
  public synchronized void assumeDerived(Version v) {	  
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Assume derived " + this + " for " + v);
    }
    Version df = v.getDynastyFounder();
    if (rootVersion == v) return;
    if (rootVersion == null) {
      rootVersion = v;
      dynastyDescendants.put(df,v);
    } else if (rootVersion.parent() == v) {
      if (!dynastyDescendants.containsKey(df)) {
        dynastyDescendants.put(df,v);
      }
      rootVersion = v;
    } else if (rootVersion.getDynastyFounder() == df) {
      if (rootVersion.comesFrom(v)) {
        rootVersion = v;
      } else {
        dynastyDescendants.put(df,v);
      }
    } else if (dynastyDescendants.containsKey(df)) {
      Version dd = dynastyDescendants.get(df);
      if (dd.comesFrom(v)) return;
      dynastyDescendants.put(df,v);
    } else {
      // difficult
      Version lca = Version.latestCommonAncestor(v,rootVersion);
      while (lca != rootVersion) {
        assumeDerived(rootVersion.parent());
      }
      if (dynastyDescendants.containsKey(df)) {
        assumeDerived(v); // easy case
        return;
      }
      Version pdf = df.parent();
      assumeDerived(pdf); // inefficient: O(n^2) where n is number of versions between lca and v.
      dynastyDescendants.put(df,v);
    }
  }
  
  /** 
   * Determine whether this thread is currently working on deriving information
   * for the given version.  This happens if the info was demanded for this
   * version or if an external tool is producing it.  If this version is
   * being derived, it is legal to mutate.
   * @param v
   * @return true if this version of the info is in the process of being derived.
   * @see #derive
   */
  public boolean isDeriving(Version v) {
    Derivation d = deriving.getValue();
    return (d != null && d.isInProcess(v)); 
  }
  
  /**
   * Determine whether we are in the process of deriving something.
   * In this case, we can't start another demand evaluation
   * or another production.
   * @return true if already in the process of deriving things.
   */
  @Override
  public boolean isDeriving() {
    return deriving.getValue() != null;
  }
  
  /** Determine whether the given version is in the subtree of
   * versions for which information has been derived.
   * This is an O(1) operation.
   * @param v version to check membership in tree
   * @return if the version is in the subtree of available versions
   */
  public boolean isDerived(Version v) {
    if (isDeriving(v)) return true;
    Version descendant;
    synchronized (this) {
      descendant = dynastyDescendants.get(v.getDynastyFounder());
    }
    return (rootVersion != null &&
            v.depth() >= rootVersion.depth() &&
            descendant != null &&
            v.depth() <= descendant.depth());
  }
  
  /**
   * Make sure that this information can be mutated for the current version.
   * Otherwise an exception is thrown.
   */
  protected void checkMutable() {
    if (!isDeriving(Version.getVersionLocal())) {
      throw new DerivationException("Cannot mutate derived information");
    }
  }

  /** Ensure that the information is available for the current version.
   * @see #ensureDerived(Version)
   * @throws UnavailableException if not derived and cannot be demanded
   * @throws DerivationException if not derived and already in the process of deriving a different version.
   */
  @Override
  public void ensureDerived() throws UnavailableException {
    Version version = Version.getVersion();
    // LOG.fine("ensureDerived " + version);
    ensureDerived(version);
  }
  
  /** This method ensures that the given version is in the subtree
   * for which information has been derived.  As necessary, it calls the derivation
   * methods to make sure this is the case.  If these methods indicate that the
   * information cannot be derived by throwing {@link UnavailableException}
   * then this method propagates this exception.
   * @param v version at which we check that information is available
   * @throws UnavailableException if not derived and cannot be derived.
   * @throws DerivationException if not derived and already in the process of deriving a different version.
   */
  public void ensureDerived(final Version v) throws UnavailableException {
	final boolean debug = LOG.isLoggable(Level.FINE);
    synchronized (this) {
      if (isDerived(v)) return;
      if (isDeriving()) { 
        Derivation d = deriving.getValue();
        throw new DerivationException(
          "Derivation " + d +
          " already in process, cannot start derivation of " + v); 
      }
      if (rootVersion == null) {
        // block while deriving the first version
    	if (debug) {
    		System.out.println("Starting "+this+" for "+v);
    	}
        derivingVersions.add(v);
        perform(new DerivingFirst(v));
        return;
      }
    }
    if (debug) {
    	LOG.fine("Deriving " + this + " for version: " + v);
    }

    Derivation derivation;

    synchronized (this) {
      // we loop until no other thread is working on the same version.
      for (;;) {
        Version lca = Version.latestCommonAncestor(rootVersion,v);
        if (!isDerived(lca)) {
          derivation = new DerivingForward(lca,v);
          derivation = new CompoundDerivation(new DerivingBackward(rootVersion,lca),derivation);
        } else if (!isDerived(v)) {
          List<Version> versions = new ArrayList<Version>();
          Version p;
          for (p = v; !isDerived(p); p = p.parent()) {
            versions.add(p);
          }
          derivation = new DerivingForward(p,versions);
        } else {
          return; // all done
        }
        if (derivingVersions.contains(derivation.firstVersion())) {
          try {
        	if (debug) {
        		System.out.println("Waiting on "+this);
        	}
            wait();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
          // repeat loop
        } else {
          if (debug) {
        	  System.out.println("Starting "+this+" for "+derivation.firstVersion());
          }
          derivingVersions.add(derivation.firstVersion());
          break;
        }
      }
    }
    
    // at this point, we know what versions we are deriving,
    // and have added the marker version to reserve the derivation space
    
    perform(derivation);
  }

  /**
   * Perform a derivation that is already given exclusive access to its versions
   * by having its first vesion marked   The derivation is processed, its marker
   * is removed and its completion is recorded.
   * @param d
   */
  private void perform(Derivation d) {
    assert deriving.getValue() == null;
    deriving.pushValue(d);
    try {
      d.process();
    } finally {
      deriving.popValue();
    }
    synchronized (this) {
      final boolean debug = LOG.isLoggable(Level.FINE);
      assert derivingVersions.contains(d.firstVersion());
      if (debug) {
    	  System.out.println("Done with "+this+" for "+d.firstVersion());
      }
      derivingVersions.remove(d.firstVersion());
      d.recordCompletion();
      if (debug) {
    	  System.out.println("notifyAll() for "+this);
      }
      notifyAll();
    }
  }
    
  private class DerivingFirst implements Derivation {
    final Version first;
    
    DerivingFirst(Version v) {
      first = v;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#firstVersion()
     */
    @Override
    public Version firstVersion() {
      return first;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#process()
     */
    @Override
    public void process() {
      deriveVersion(first);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#recordCompletion()
     */
    @Override
    public void recordCompletion() {
      setRootVersion(first);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#isInProcess(edu.cmu.cs.fluid.version.Version)
     */
    @Override
    public boolean isInProcess(Version v) {
      return v == first;
    }
    
    @Override
    public String toString() {
      return "DerivingFirst(" + first + ")";
    }
  }
  
  private class DerivingBackward implements Derivation {
    final Version from, to;
    Version curr;

    DerivingBackward(Version f, Version t) {
      from = f;
      to = t;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#firstVersion()
     */
    @Override
    public Version firstVersion() {
      return from;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#process()
     */
    @Override
    public void process() {
      final boolean debug = LOG.isLoggable(Level.FINE);
      Version p;
      for (p = from; p != to; p = curr) {
        curr = p.parent();
        if (debug) {
        	LOG.fine(VersionedDerivedInformation.this + ": About to derive up from " + p + " to " + curr);
        }
        deriveParent(p,curr);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#recordCompletion()
     */
    @Override
    public void recordCompletion() {
      Version p;
      for (p = from; p != to; ) {
        p = p.parent();
        assumeDerived(p);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#isInProcess(edu.cmu.cs.fluid.version.Version)
     */
    @Override
    public boolean isInProcess(Version v) {
      return v == curr || curr != null && from.comesFrom(v) && v.comesFrom(curr);
    }
  }
  
  private class DerivingForward implements Derivation {
    final List<Version> versions;
    final Version from;
    Version curr;
    
    DerivingForward(Version f, Version t) {
      from = f;
      versions = new ArrayList<Version>();
      for (Version p = t; p != f; p = p.parent()) {
        versions.add(p);
      }
    }
    
    DerivingForward(Version f, /* @unique */ List<Version> path) {
      from = f;
      versions = path;
    }
    
    @Override
    public Version firstVersion() {
      return versions.get(versions.size()-1);
    }

    @Override
    public void process() {
      final boolean debug = LOG.isLoggable(Level.FINE);
      Version prev = from;
      for (ListIterator<Version> it = versions.listIterator(versions.size()); it.hasPrevious();) {
        curr = it.previous();
        if (debug) {
          LOG.fine(VersionedDerivedInformation.this + ": About to derive down from " + prev + " to " + curr);
        }
        deriveChild(prev,curr);
        prev = curr;
      }
    }

    @Override
    public void recordCompletion() {
      for (ListIterator<Version> it = versions.listIterator(versions.size()); it.hasPrevious();) {
        assumeDerived(it.previous());
      }
    }

    @Override
    public boolean isInProcess(Version v) {
      return curr != null && curr.comesFrom(v) && v.comesFrom(from);
    }
  }
  
  private static class CompoundDerivation implements Derivation {
    Derivation deriv1, deriv2;
    CompoundDerivation(Derivation d1, Derivation d2) {
      deriv1 = d1;
      deriv2 = d2;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#firstVersion()
     */
    @Override
    public Version firstVersion() {
      return deriv1.firstVersion();
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#process()
     */
    @Override
    public void process() {
      deriv1.process();
      deriv2.process();
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#recordCompletion()
     */
    @Override
    public void recordCompletion() {
      deriv1.recordCompletion();
      deriv2.recordCompletion();
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation.Derivation#isInProcess(edu.cmu.cs.fluid.version.Version)
     */
    @Override
    public boolean isInProcess(Version v) {
      return deriv1.isInProcess(v) || deriv2.isInProcess(v);
    }
  }
  
  /** Make sure derived information is valid for this version.
   * This version (in {@link VersionedDerivedInformation}) simply
   * throws an exception and thus if a subclass intends only to provide 
   * information eagerly, this method can be left as-is.
   * This method is called from contexts where the information is mutable for this version.;
   * @param v version at which information should be made available
   * @throws UnavailableException if information cannot be derived for some reason.
   */
  protected void deriveVersion(Version v) throws UnavailableException {
    throw new UnavailableException("Cannot derive information on demand for " + v);
  }
  
  /**
   * Derive information for a version given that information is already available
   * for one child version.  By default this method simply calls {@link #deriveVersion}.
   * @param parent version for which information should be computed;
   *        the information is mutable for this version.
   * @param child version for which information is already computed; this is a child
   * @throws UnavailableException if information cannot be computed
   */
  protected void deriveParent(Version child, Version parent) throws UnavailableException {
    deriveVersion(parent);
  }
  
  /**
   * Derive information for all versins above an existing derived version up
   * to and including the ancestor version.  The state is mutable for all these
   * versions in the context in which this method is called.
   * <p>
   * This implementation simply calls {@link #deriveParent} for all versions up
   * to the ancestor.
   * @param child existing derived version
   * @param ancestor version for which information should be derived, including
   *        all intermediate versions.
   * @throws UnavailableException if information cannot be computed.
   */
  protected final void deriveAncestor(Version child, Version ancestor) throws UnavailableException {
    while (child != ancestor) {
      Version p = child.parent();
      deriveParent(child,p);
      child = p;
    }
  }
  
  /**
   * Derive information for a version given that information is already available
   * for its parent version.  By default this method simply calls {@link #deriveVersion}.
   * @param child version for which information should be computed
   * @param parent version for which information is already computed; this is the parent
   * @throws UnavailableException if information cannot be computed
   */
  protected void deriveChild(Version parent, Version child) throws UnavailableException {
    deriveVersion(child);
  }

  /**
   * Derive information for a chain of versions from an existing derived version
   * down to the given descendant.  All the versions are mutable when this method is called.
   * <p>
   * This implementation simply calls {@link #deriveChild} for each version.
   * @param ancestor derived version
   * @param descendant descendant version of the ancestor
   * @throws UnavailableException if information cannot be computed on demand
   */
  protected final void deriveDescendant(Version ancestor, Version descendant) 
    throws UnavailableException
  {
    // special case:
    if (descendant.parent() == ancestor) {
      deriveChild(ancestor,descendant);
      
      // another special case:
    } else if (descendant.getDynastyFounder() == ancestor.getDynastyFounder()) {
      while (ancestor != descendant) {
        Version child = ancestor.getNextInDynasty();
        deriveChild(ancestor,child);
        ancestor = child;
      }
      
      // the inefficient case (should be rare)
    } else {
      // need to access versions in opposite order
      Stack<Version> s = new Stack<Version>();
      while (descendant != ancestor) {
        s.push(descendant);
        descendant = descendant.parent();
      }
      Version parent = ancestor;
      while (!s.isEmpty()) {
        Version child = s.pop();
        deriveChild(parent,child);
        parent = child;
      }
    }
  }
  
  public String debugString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Root = " + rootVersion + "\n");
    sb.append("Deriving + " + deriving.getValue()+"\n");
    sb.append("deriving versions:");
    for (Version v : derivingVersions) {
      sb.append(" "); sb.append(v);
    }
    sb.append("\nDerived max for dynasty:\n");
    for (Map.Entry<Version,Version> e : dynastyDescendants.entrySet()) {
      sb.append("  ");
      sb.append(e.getKey());
      sb.append(" -> ");
      sb.append(e.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }
}
