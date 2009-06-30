/*$Header$*/
package com.surelogic.tree;

import java.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaFileStatus;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.tree.*;

public class SyntaxTreeNode extends JavaNode {// PlainIRNode {
  /**
   * These will either be initialized by JavaNode, or null
   */
  IRSequence<IRNode> children; 
  IRNode parent; 
  IRLocation loc; 
  Operator op;
  ISrcRef srcRef; // Added for free, due to rounding
  String info;
  
  public SyntaxTreeNode(Operator op, IRNode[] children) {
    super(tree, op, children);
    initFields();
  }
  
  public SyntaxTreeNode(Operator op) {
    super(tree, op);
    initFields();
  }

  private void initFields() {
    /*
     * Initialize to undefined value if not defined by JavaNode
     */
    if (this.children == null) {
      this.children = Constants.undefinedSequence;
    }
    if (this.parent == null) {
      this.parent = Constants.undefinedNode;
    }
    if (this.loc == null) {
      this.loc = Constants.undefinedLocation;
    }
    if (this.op == null) {
      this.op = Constants.undefinedOperator;
    }
    if (this.info == null) {
        this.info = Constants.undefinedString;
    }
  }
  
  @Override
  public String toString() {
	  return super.toString(); // +" "+DebugUnparser.toString(this);
  }
  
  @SuppressWarnings("unchecked")
  static Set<SlotInfo> noticed = new HashSet<SlotInfo>();
  
  /*
  @Override
  public <T> void setSlotValue(SlotInfo<T> si, T newValue) {
	  try {
		  super.setSlotValue(si, newValue); 
	  } finally {		  
		  if (si instanceof NodeStoredSlotInfo) {
			  // Nothing to do
		  }
		  else if (si.getBundle() == null || JavaFileStatus.isPersisted(si)) {
			  if (!noticed.contains(si)) {
				  System.out.println("Problem with "+si.name());
				  noticed.add(si);
			  }
		  }
	  }
  }
  */
}
