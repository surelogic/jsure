/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TreeChanged.java,v 1.20 2007/07/10 22:16:32 aarong Exp $ */
package edu.cmu.cs.fluid.version;

import java.util.Observable;
import java.util.Observer;

import edu.cmu.cs.fluid.ir.ChangeRecord;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.tree.*;

/** Change bits attached to tree nodes.  If a change is noted at
 * a node, it is propagated toward the root.  At any point,
 * a client can request if a change has happened at a node
 * (which of course includes any node in its subtree)
 * with respect to any prior version.
 * @deprecated Use {@link edu.cmu.cs.fluid.ir.ChangeRecord} and {@link edu.cmu.cs.fluid.tree.PropagateUpTree}
 */
@Deprecated
public class TreeChanged extends VersionedUnitSlotInfo implements Observer, ChangeRecord {

  final MutableTreeInterface tree;

  public TreeChanged(MutableTreeInterface t) {
    tree = t;
    t.addObserver(this);
  }

  public TreeChanged(String name, MutableTreeInterface t) throws SlotAlreadyRegisteredException {
    super(name);
    tree = t;
    t.addObserver(this);
  }

  /** A separate object holder observers of roots. */
  private final Observable rootObservable = new Observable() {
    @Override
    public void notifyObservers(Object o) {
      super.setChanged();
      super.notifyObservers(o);
    }
  };

  public void addRootObserver(Observer o) {
    rootObservable.addObserver(o);
  }

  public void deleteRootObserver(Observer o) {
    rootObservable.deleteObserver(o);
  }

  /** Note a change at a node and at all its ancestors. */
  public void noteChange(IRNode node) {
    while (node != null && setChanged(node)) {
      // System.out.println("Setting tree changed for " + node);
      IRNode parent = tree.getParentOrNull(node);
      if (parent == null) {
        rootObservable.notifyObservers(node);
      }
      node = parent;
    }
  }

  /** Inform a change at a particular IRNode.
   * @param node a tree node
   */
  @Override
  public void update(Observable obs, Object node) {
    noteChange((IRNode) node);
  }

  /**
   * Return an iterator over all nodes in a subtree that have tree-changed
   * marks.  This may be because the node itself is changed (new structure here,
   * or other local changes) or because some descendant node has changed.
   * Note, we traverse the tree in version v2, and thus will not traverse
   * nodes that were deleted from v1 and v2.  Thus it may be necessary to
   * traverse two iterators: this one and one with the versions swapped.
   * @param root root of the subtree to investigate
   * @param v1 old version to compare against
   * @param v2 new version to use to traverse the tree
   * @return iterator over changed nodes. Also legally castable to Enumeration.
   *
  public Iterator<IRNode> iterator(final IRNode root, final Version v1, final Version v2) {
    return TreeChangedIterator.iterator(this, tree, root, v1, v2);
  }*/
}