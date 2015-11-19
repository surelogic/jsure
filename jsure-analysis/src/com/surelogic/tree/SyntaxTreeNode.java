/*$Header$*/
package com.surelogic.tree;

import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.SkeletonJavaRefUtility.JavaRefSkeletonBuilder;
import edu.cmu.cs.fluid.java.operator.IllegalCode;
import edu.cmu.cs.fluid.tree.*;

public class SyntaxTreeNode extends JavaNode {// PlainIRNode {
  private static final long serialVersionUID = 1L;

  /**
   * These will either be initialized by JavaNode, or null
   */
  // Want an even number of 4-byte fields to fit in the 8-byte cache line
  IRSequence<IRNode> children; 
  IRNode parent; 
  
  Operator op;
  IRNode granule;
  
  Integer modifiers;
  String info;
  
  IJavaRef srcRef;
  IRLocation loc;
  
  public SyntaxTreeNode(Operator op, IRNode[] children) {
    super(tree, op, children);
    initFields();
  }
  
  public SyntaxTreeNode(Operator op, int min) {
	super(tree, op, min);
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

  @SuppressWarnings("serial")
  static class IllegalNode extends SyntaxTreeNode {
	  public IllegalNode(Operator op, int min) {
		  super(op, min);
	  }
	  public IllegalNode(Operator op, IRNode[] children) {
		  super(op, children);
	  }			  
	  public IllegalNode(Operator op) {
		  super(op);	  
	  }
  }
  
  public static SyntaxTreeNode create(Operator op, IRNode[] children) {
	  if (op instanceof IllegalCode) {
		  if (children != null) {
			  return new IllegalNode(op, children);
		  } else {
			  return new IllegalNode(op);
		  }
	  }
	  if (children != null) {
		  return new SyntaxTreeNode(op, children);
	  } else {
		  return new SyntaxTreeNode(op);
	  }
  }
  
  // Used for copying
  public static IRNode create(Operator op, int min) {
	if (op instanceof IllegalCode) {
	  throw new IllegalArgumentException("Bad operator for new node: "+op);
	}
	return new SyntaxTreeNode(op, min);
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
    if (this.granule == null) {
    	this.granule = Constants.undefinedNode;
    }
  }
  
  @Override
  public String toString() {
	  return super.toString(); // +" "+DebugUnparser.toString(this);
  }
  
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
	  granule = null;
	  //destroyed.add(this.toString());
  }
  /*
  @SuppressWarnings("unchecked")
  static Set<SlotInfo> noticed = new HashSet<SlotInfo>();

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
  /*
  static final Set<String> destroyed = new ConcurrentHashSet<String>();
  
  @Override
  protected void finalize() throws Throwable {	  
	  if (destroyed.contains(this.toString())) {
		  System.out.println(this);
	  }
	  super.finalize();
  }
  */
  
  public static JavaRefSkeletonBuilder getSkeletonBuilder(IRNode n) {
	  if (n instanceof SyntaxTreeNode) {
		  SyntaxTreeNode s = (SyntaxTreeNode) n;
		  IJavaRef ref;
		  synchronized (s) {
			ref = s.srcRef;
		  }
		  if (ref instanceof JavaRefSkeletonBuilder) {
			  return (JavaRefSkeletonBuilder) ref;
		  }
	  }
	  return null;
  }
}
