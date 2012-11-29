/*$Header$*/
package com.surelogic.tree;

import java.util.*;

import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.tree.*;

public class SyntaxTreeNode extends JavaNode {// PlainIRNode {
  private static final long serialVersionUID = 1L;

  /**
   * These will either be initialized by JavaNode, or null
   */
  IRSequence<IRNode> children; 
  IRNode parent; 
  Operator op;
  Integer modifiers; // Added on x64, more to avoid lock contention
  IJavaRef srcRef; // Added for free on x86, due to rounding for cache line alignment
  String info;               
  IRLocation loc; 
  
  public SyntaxTreeNode(Operator op, IRNode[] children) {
    super(tree, op, children);
    initFields();
  }
  
  public SyntaxTreeNode(Operator op) {
    super(tree, op);
    initFields();
  }
  
  public SyntaxTreeNode() {
	super(tree);
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
    if (this.modifiers == null) {
    	this.modifiers = Constants.undefinedInteger;
    }
  }
  
  @Override
  public String toString() {
	  return super.toString(); // +" "+DebugUnparser.toString(this);
  }
  
  @SuppressWarnings("unchecked")
  static Set<SlotInfo> noticed = new HashSet<SlotInfo>();
  
  @Override
  public synchronized void destroy() {
	  super.destroy();
	  children = null;
	  info = null;
	  loc = null;
	  op = null;
	  parent = null;
	  srcRef = null;
	  modifiers = null;
  }
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
