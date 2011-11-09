/*
 * Created on Jan 25, 2005
 *
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.sea.*;

/**
 * @author Edwin
 *
 */
public abstract class AbstractDropFactory<D extends PromiseDrop,V> implements IDropFactory<D,V> {
  protected final SlotInfo<D> si;
  private final DependencyType depType;
  
  protected AbstractDropFactory(String tag) {
    this(DependencyType.DEFAULT, tag);
  }
  protected AbstractDropFactory(DependencyType t, String tag) {
    depType = t;
    if (tag == null) {
      si = SimpleSlotFactory.prototype.newAttribute(null);
    } else {
      si = AbstractPromiseAnnotation.makeDropSlotInfo(tag);
    }
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IDropFactory#getSI()
   */
  public final SlotInfo<D> getSI() { 
    return si;
  }
  
  public final D getDrop(final IRNode n, V val) {
    D drop = n.getSlotValue(si);
    if (drop != null) {
      return drop;
    }
    return createDrop(n, val);
  }
    
  public final D createDrop(final IRNode n, V val) {
    D drop = newDrop(n, val);
    if (drop != null) {
      drop.setAttachedTo(n, si);
      setDependencies(n, val, drop);
      return drop;
    }
    return null;
  }

  enum DependencyType { 
    DEFAULT,   // Set node and comp unit
    ONLY_CU,   // Only set comp unit
    ONLY_NODE, // Only set node
    NONE 
  }
  protected void setDependencies(IRNode n, V val, D drop) {
    switch (depType) {
    case DEFAULT:
      drop.setNodeAndCompilationUnitDependency(n);
      return;
    case ONLY_CU:
      drop.dependUponCompilationUnitOf(n);
      return;
    case ONLY_NODE:
      drop.setNode(n);
      return;
    case NONE:
    }
  }
  
  /**
   * Create the specific drop
   * 
   * @param n The node that the drop is associated with
   * @return
   */
  protected abstract D newDrop(IRNode n, V val);
}
