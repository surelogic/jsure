/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TreeChangedIterator.java,v 1.2 2007/07/10 22:16:32 aarong Exp $*/
package edu.cmu.cs.fluid.version;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.TreeInterface;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;
import edu.cmu.cs.fluid.util.EmptyIterator;

public class TreeChangedIterator extends AbstractRemovelessIterator<IRNode>
{

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
   */
  public static Iterator<IRNode> iterator(VersionedChangeRecord rec, TreeInterface tree, final IRNode root, final Version v1, final Version v2) {
    if (!rec.changed(root, v1, v2)) { return new EmptyIterator<IRNode>(); }
    // otherwise root has changed, so ...
    return new TreeChangedIterator(rec, tree, root, v1, v2);
  }

  /**
   * 
   */
  private final VersionedChangeRecord changeRecord;
  private final TreeInterface tree;
  private final Version v1, v2;

  private TreeChangedIterator(VersionedChangeRecord changed, TreeInterface tree, IRNode root, Version v1, Version v2) {
    changeRecord = changed;
    this.tree = tree;
    this.v1 = v1;
    this.v2 = v2;
    setNext(root);
  }

  /**
   * Stack of enumerations of children
   */
  private Stack<Iterator<IRNode>> toVisit = new Stack<Iterator<IRNode>>();

  private void setNext(IRNode node) {
    next = node;
    Version.saveVersion(v2);
    try {
      toVisit.push(tree.children(node));
    } finally {
      Version.restoreVersion();
    }
  }

  private IRNode next;

  private void findNext() {
    while (!toVisit.isEmpty()) {
      Iterator<IRNode> enm = toVisit.peek();
      while (enm.hasNext()) {
        IRNode n = enm.next();
        if (changeRecord.changed(n, v1, v2)) {
          setNext(n);
          return;
        }
      }
      toVisit.pop();
    }
    next = null;
  }

  public boolean hasNext() {
    return next != null;
  }

  public IRNode next() {
    if (next == null) throw new NoSuchElementException("no more changed nodes");
    IRNode n = next;
    findNext();
    return n;
  }
}