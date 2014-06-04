package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/** This abstract class covers all edges that cross
 * VariableSubcomponent boundaries.  Each edge must be registered
 * with the VariableSubcomponent in question at a particular index.
 * Each edge has two parts: one entering one component and one exiting
 * another (previous or next) component.  A special component is
 * placed around the whole sequence to keep the ends together.
 */

public class VariableSubcomponentControlEdge extends ControlEdge {
  protected VariableSubcomponent subcomponent;
  protected int index;
  protected boolean isEntry;
  protected ControlNode local;
  protected boolean isSecondary;
  protected boolean isReverse;
  
  public VariableSubcomponentControlEdge
      (VariableSubcomponent vs, int i, boolean entryp, ControlNode local) 
  {
	  this(vs,i,entryp,local,false);
  }

  /**
   * Create a variable subcomponent control edge that (possibly) runs in the opposite direction:
   * after exiting a node we go to the previous one, not the next one.
   * @param vs the component we are associated with.
   * @param i which chain to choose (there may be multiple chains through the subcomponent)
   * @param entryp is this the entry into the component, or exit?
   * @param local the local node to attach to (other node is implicit)
   * @param rev true if in reverse, false for a normal situation
   */
  public VariableSubcomponentControlEdge
  (VariableSubcomponent vs, int i, boolean entryp, ControlNode local, boolean rev)
  {
	  if (local == null) throw new IllegalArgumentException("VSE local node must not be null");
	  isReverse = rev;
	  subcomponent = vs;
	  index = i;
	  isEntry = entryp;
	  vs.registerVariableEdge(this,i,entryp);
	  if (entryp) {
		  attachSink(local);
	  } else {
		  attachSource(local);
	  }
  }

  public VariableSubcomponent getVariableSubcomponent() {
    return subcomponent;
  }
  public int getIndex() {
    return index;
  }
  public boolean getIsEntry() {
    return isEntry;
  }
  
  /** The the other half of the edge.
   * It will be another variable edge of the opposite type.
   * (If the subcomponent has null loc, that means
   *  it is the special subcomponent that sits at the
   *  beginning and ending of the sequence).
   */
  public VariableSubcomponentControlEdge getDual() {
    IRLocation loc = subcomponent.getLocation();
    Component comp = subcomponent.getComponent();
    ISubcomponent sub;
    IRNode parent = comp.getSyntax();
    if (loc == null) { // get first/last if existing
      if (isEntry != isReverse) {
        loc = Subcomponent.tree.lastChildLocation(parent);
      } else {
        loc = Subcomponent.tree.firstChildLocation(parent);
      }
    } else {
      if (isEntry != isReverse) {
        loc = Subcomponent.tree.prevChildLocation(parent, loc);
      } else {
        loc = Subcomponent.tree.nextChildLocation(parent, loc);
      }
    }
    if (loc != null) {
      sub = comp.getSubcomponent(loc);      
    } else {
      sub = comp.getVariableSubcomponent();
    }
    return sub.getVariableEdge(index,!isEntry);
  }

  /** The entry and exit edges are the "same" node
   * and so we defer all IR equality tests and slot values
   * from entry to exit edges.
   */
  @Override public Object identity() {
    if (isEntry) {
      return getDual();
    } else {
      return this;
    }
  }

  @Override public boolean equals(Object other) {
    if (isEntry) {
      return getDual().equals(other);
    } else {
      return super.equals(other);
    }
  }

  @Override public int hashCode() {
    if (isEntry) {
      return getDual().hashCode();
    } else {
      return super.hashCode();
    }
  }

  /** Return the value of an attribute.
   * Values are always stored on the exit part of an edge.
   */
  @Override public <T> T getSlotValue(SlotInfo<T> si) {
    if (isEntry) {
      return getDual().getSlotValue(si);
    } else {
      return super.getSlotValue(si);
    }
  }
  
  /** Set the value of an attribute.
   * Values are always stored on the exit part of an edge.
   */
  @Override public <T> void setSlotValue(SlotInfo<T> si, T value) {
    if (isEntry) {
      getDual().setSlotValue(si,value);
    } else {
      super.setSlotValue(si,value);
    }
  }
  
  @Override public ControlNode getSource() {
    if (isEntry) {
      return getDual().local;
    } else {
      return local;
    }
  }
  
  @Override public ControlNode getSink() {
    if (isEntry) {
      return local;
    } else {
      return getDual().local;
    }
  }
  
  @Override public boolean sourceIsSecondary() {
    if (isEntry) {
      return getDual().isSecondary;
    } else {
      return isSecondary;
    }
  }

  @Override public boolean sinkIsSecondary() {
    if (isEntry) {
      return isSecondary;
    } else {
      return getDual().isSecondary;
    }
  }
  
  @Override
  protected void setSource(ControlNode source, boolean second) 
      throws EdgeLinkageError
  {
    if (isEntry) {
      getDual().setSource(source, second); // throw new EdgeLinkageError("no source explicit");
    } else {
      local = source;
      isSecondary = second;
    }
  }

  @Override
  protected void setSink(ControlNode sink, boolean second) 
      throws EdgeLinkageError
  {
    if (isEntry) {
      local = sink;
      isSecondary = second;
    } else {
      getDual().setSink(sink, second); // throw new EdgeLinkageError("no sink explicit");
    }
  }
  
  @Override public String toString() {
	  return super.toString() + (isEntry ? "/entry/" : "/exit/") + index;
  }
}
