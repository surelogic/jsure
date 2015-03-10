/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SimpleChangeRecord.java,v 1.1 2007/05/25 02:12:41 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.HashSet;

/**
 * A change record that simple lists nodes that are changed.
 * For efficiency, nothing is remembered until {@link #clearChanges()} is called.
 * For correctness, this means we assume everything is changed until 
 * {@link #clearChanges()} is called.
 * @author boyland
 */
public class SimpleChangeRecord extends AbstractChangeRecord {
  private HashSet<IRNode> changes;
  
  /**
   * Create a non-registered simple record of changes.
   */
  public SimpleChangeRecord() {
  }

  /**
   * Create a registered record of changes.
   * @param name
   * @throws SlotAlreadyRegisteredException
   */
  public SimpleChangeRecord(String name) throws SlotAlreadyRegisteredException {
    super(name);
    // TODO Auto-generated constructor stub
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.ChangeRecord#clearChanges()
   */
  @Override
  public void clearChanges() {
    synchronized (this) {
      if (changes == null) changes = new HashSet<IRNode>();
      else changes.clear();
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.ChangeRecord#isChanged(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public boolean isChanged(IRNode node) {
    synchronized (this) {
      return changes == null || changes.contains(node);
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.ChangeRecord#setChanged(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public boolean setChanged(IRNode node) {
    synchronized (this) {
      if (changes == null) return false;
      return changes.add(node);
    }
  }

}
