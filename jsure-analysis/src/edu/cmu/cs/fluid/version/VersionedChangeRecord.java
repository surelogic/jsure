/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedChangeRecord.java,v 1.2 2007/07/10 22:16:33 aarong Exp $*/
package edu.cmu.cs.fluid.version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;
import java.util.Map;
import java.util.Vector;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.AbstractChangeRecord;
import edu.cmu.cs.fluid.ir.ChangeRecord;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeHashedMap;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.tree.TreeInterface;

/**
 * Change information recorded by version.
 * @author boyland
 */
public class VersionedChangeRecord extends AbstractChangeRecord implements ChangeRecord, Observer {
  private final IRNodeHashedMap<VersionSet> changeLog = new IRNodeHashedMap<VersionSet>();
  
  private List<IRNode> backlog1 = new ArrayList<IRNode>();
  private List<Version> backlog2 = new ArrayList<Version>();
  private boolean clearing = false;
  
  private Version source;
  
  
  public VersionedChangeRecord() {
    super();
    // TODO Auto-generated constructor stub
  }

  public VersionedChangeRecord(String name) throws SlotAlreadyRegisteredException {
    super(name);
    // TODO Auto-generated constructor stub
  }

  /**
   * Record the current state as "unchanged".
   * After this call, all calls to {@link #isChanged(IRNode)} will return false,
   * until explicit set as changed.
   * <p>
   * Warning: there is another function {@link #clearChanged()} that has a completely
   * different function and which shouldn't be used.
   */
  @Override
  public void clearChanges() {
    source = Version.getVersion();
  }
  
  /**
   * Return whether any changes have been recorded for this node.
   * @param node
   * @return true if we have recorded a change for this node (since the last clear).
   */
  @Override
  public boolean isChanged(IRNode node) {
    if (source != null) return changed(node,source);
    else return changed(node,Version.getVersion().parent());
  }
  
  /**
   * Record that something has changed for this node.
   */
  @Override
  public boolean setChanged(IRNode node) {
    if (VersionedSlot.getEra() != null) {
      List<Version> eraChanges = VersionedSlot.getEraChanges();
      if (node == null) {
        // back-channel communication from StoredSlotInfo
        eraChanges.clear();
        // System.out.println("Clearing changes.");
        return false;
      }
      // System.out.println("Got some changes for " + node + ": " + eraChanges);
      synchronized (changeLog) {
        for (Version v : eraChanges) {
          backlog1.add(node);
          backlog2.add(v);
        }
      }
      return false;
    } else {
      return setChanged(node, Version.getVersionLocal());
    }
  }

  protected void clearBacklog() {
    if (VersionedSlot.getEra() != null) {
      System.err.println("!! clearBacklog called at bad time");
      return;
    }
    synchronized (changeLog) {
      if (clearing) return;
      Version.saveVersion();
      clearing = true;
      try {
        int n = backlog1.size();
        for (int i = 0; i < n; ++i) {
          Version v = backlog2.get(i);
          Version.setVersion(v);
          // System.out.println(" Clearing " + v + ": " + backlog1.get(i));
          setChanged(backlog1.get(i), v);
        }
        backlog1.clear();
        backlog2.clear();
      } finally {
        clearing = false;
        Version.restoreVersion();
      }
    }    
  }

  private boolean setChanged(IRNode node, Version v) {
    if (v.parent() != null && changed(node, v, v.parent())) {
      // System.out.println(" Already changed, ignored: " + v + ": " + node);
      return false;
    } else {
      synchronized (changeLog) {
        VersionSet vs = changeLog.get(node);
        if (vs == null) {
          vs = SingletonVersionSet.create(v); // we should have a more efficient method of storage
        } else {
          vs = vs.add(v);
        }
        changeLog.put(node, vs);
      }
      super.setChanged();
      super.notifyObservers(node);
      return true;
    }
  }

  /**
   * Return true if a change has been noted for the node between the current
   * version and the given version.
   */
  public boolean changed(IRNode node, Version other) {
    return changed(node, other, Version.getVersionLocal());
  }
  
  /**
   * Return true if a change has been noted for the node between two versions.
   */
  public boolean changed(IRNode node, Version v1, Version v2) {
    clearBacklog();
    if (v1.equals(v2))
      return false;
    synchronized (changeLog) {
      VersionSet vs = changeLog.get(node);
      if (vs == null) return false;
      return vs.changed(v1,v2);
    }
  }

  @Override
  public void addObserver(Observer o) {
    super.addObserver(o);
    clearBacklog();
    synchronized (changeLog) {
      //XXX: very expensive
      Version.saveVersion();
      try {
        for (Map.Entry<IRNode,VersionSet> e : changeLog.entrySet()) {
          for (Version v : e.getValue().changes()) {
            Version.setVersion(v);
            o.update(this, e.getKey());
          }
        }
      } finally {
        Version.restoreVersion();
      }
    }
  }
  
  public Iterator<IRNode> iterator(TreeInterface tree, final IRNode root, final Version v1, final Version v2) {
    return TreeChangedIterator.iterator(this, tree, root, v1, v2);
  }
}

interface VersionSet {

  public abstract VersionSet add(Version v);
  
  public abstract boolean changed(Version v1, Version v2);
  
  public abstract Iterable<Version> changes();
}

class EmptyVersionSet implements VersionSet {
  public static EmptyVersionSet prototype = new EmptyVersionSet();
  private static Iterable<Version> noVersions = new EmptyIterator<Version>();

  @Override
  public VersionSet add(Version v) {
    return SingletonVersionSet.create(v);
  }

  @Override
  public boolean changed(Version v1, Version v2) {
    return false;
  }
  
  @Override
  public Iterable<Version> changes() {
    return noVersions;
  }
}

class SingletonVersionSet implements VersionSet {
  private static Version lastVersion;
  private static SingletonVersionSet lastSlot;

  private Version version;

  public static SingletonVersionSet create(final Version v) {
    if (lastVersion == v) {
      return lastSlot;
    } else {
      lastVersion = v;
      lastSlot = new SingletonVersionSet(v);
      return lastSlot;
    }
  }

  private SingletonVersionSet(Version v) {
    version = v;
  }

  @Override
  public VersionSet add(Version v) {
    Version current = v;
    if (version == current)
      return this;
    
    return new ManyVersions(version).add(v);
  }

  @Override
  public boolean changed(Version v1, Version v2) {
    return version.isBetween(v1, v2);
  }

  @Override
  public Iterable<Version> changes() {
    return new SingletonIterator<Version>(version);
  }
}

class ManyVersions implements VersionSet {
  static int count;
  private final Vector<Version> versionLog;

  public ManyVersions(Version v) {
    versionLog = new Vector<Version>();
    versionLog.addElement(v);
    count++;
  }
  /**
   * Create a set of versions all from the same era, already in sorted order.
   */
  ManyVersions(Vector<Version> vlog) {
    versionLog = vlog;
  }

  protected final int findVersion(Version version) {
    int min = 0;
    int max = versionLog.size();
    int index;

    // look for version in log:
    while (min < max) {
      index = (min + max) / 2;
      Version v = versionLog.elementAt(index);
      if (version.equals(v))
        return index;
      if (version.precedes(v)) {
        max = index;
      } else {
        min = index + 1;
      }
    }
    return min;
  }

  @Override
  public VersionSet add(Version v) {
    addVersion(v);
    return this;
  }

  void addVersion(Version current) {
    // TODO: See below: perhaps we should just use a HashSet
    int index = findVersion(current);

    if (index < versionLog.size()
      && // I wish I didn't have to test again...
    versionLog.elementAt(
      index).equals(
        current))
      return;

    versionLog.insertElementAt(current, index);
    /*
     * System.out.print("Log: "); for (int i=0; i < versionLog.size(); ++i)
     * System.out.print(versionLog.elementAt(i)+ " "); System.out.println();
     */
  }

  @Override
  public boolean changed(Version v1, Version v2) {
    // TODO: If this code is slow, we can try substituting with a HashSet.
    Version lca = Version.latestCommonAncestor(v1, v2);
    Version end = Version.precedes(v1, v2) ? v2 : v1;

    int lcaindex = findVersion(lca);
    int endindex = findVersion(end);

    /*
     * we have to special case the final case or else turn the following for
     * loop to test i <= endindex and then first ensure endindex in range.
     */
    if (endindex >= lcaindex
      && endindex < versionLog.size()
      && !v1.equals(v2)
      && versionLog.elementAt(endindex).equals(end))
      return true;

    for (int i = lcaindex; i < endindex; ++i) {
      Version v = versionLog.elementAt(i);
      if (v.isBetween(v1, v2))
        return true;
    }

    return false;
  }

  @Override
  public Iterable<Version> changes() {
    return versionLog;
  }
}