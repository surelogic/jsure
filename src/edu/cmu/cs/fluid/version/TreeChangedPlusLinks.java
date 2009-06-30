/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TreeChangedPlusLinks.java,v 1.2 2007/07/06 18:31:10 chance Exp $*/
package edu.cmu.cs.fluid.version;

import java.util.TreeSet;
import java.util.Collection;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeHashedMap;
import edu.cmu.cs.fluid.tree.MutableTreeInterface;


/**
 * An implementation of TreeChanged that keeps remote links as well.
 * @author boyland
 */
@SuppressWarnings("deprecation")
public class TreeChangedPlusLinks extends TreeChanged {

  private final IRNodeHashedMap<Collection<IRNode>> links = new IRNodeHashedMap<Collection<IRNode>>();
  
  /**
   * @param t
   */
  public TreeChangedPlusLinks(MutableTreeInterface t) {
    super(t);
  }

  @Override
  public synchronized boolean setChanged(IRNode node) {
    if (super.setChanged(node)) {
      Collection<IRNode> l = links.get(node);
      if (l == null) return true;
      for (IRNode n : l) {
        noteChange(n);
      }
      return true;
    }
    return false;
  }
  
  public synchronized void addLink(IRNode node, IRNode link) {
    Collection<IRNode> l = links.get(node);
    if (l == null) {
      l = new TreeSet<IRNode>();
    }
    l.add(node);
    Version.saveVersion();
    try {
      for (Version v : changes(node)) {
        Version.setVersion(v);
        setChanged(link);
      }
    } finally {
      Version.restoreVersion();
    }
  }
}
