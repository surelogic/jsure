/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRNodeType.java,v 1.13 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.NotImplemented;
import com.surelogic.ThreadSafe;

/** The type of IR nodes.  Needed for persistence.
 */
@ThreadSafe
public class IRNodeType implements IRType<IRNode> {
  // private Operator op;
  
  public IRNodeType() {}

  public static final IRNodeType prototype = new IRNodeType();
  static {
    IRPersistent.registerIRType(prototype,'N');
  }
  
  public boolean isValid(Object x) {
    return x == null || x instanceof IRNode;
  }

  public Comparator<IRNode> getComparator() 
  {
    return null;
  }

  public void writeValue(IRNode v, IROutput out) 
     throws IOException
  {
    out.writeNode(v);
  }
  public IRNode readValue(IRInput in)
     throws IOException
  {
    return in.readNode();
  }
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('N');
  }
  public IRType<IRNode> readType(IRInput in) { return this; }

  /** @exception fluid.NotImplemented */
  public IRNode fromString(String s) {
    throw new NotImplemented("fluid.ir.IRNodeType.fromString()");
  }

  /** @exception fluid.NotImplemented */
  public String toString(IRNode o) {
    throw new NotImplemented("fluid.ir.IRNodeType.toString()");
  }
}
