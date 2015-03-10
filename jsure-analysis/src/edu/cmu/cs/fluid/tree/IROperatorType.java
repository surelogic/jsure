/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/IROperatorType.java,v 1.13 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.ir.*;

/** The type of operators of tree nodes.
 * @see Operator
 * @see Tree
 */

public class IROperatorType extends CachedType<Operator> {
  private IROperatorType() {}
  public static final IROperatorType prototype = new IROperatorType();
  static { IRPersistent.registerIRType(prototype,'O'); }

  @Override
  public boolean isValid(Object x) {
    return x == null || x instanceof Operator;
  }

  @Override
  public Comparator<Operator> getComparator() 
  {
    return null;
  }

  @Override
  protected void writeValueInternal(Object value, IROutput out)
       throws IOException
  {
    Operator op = (Operator)value;
    out.writeUTF(op.internalName());
    op.writeInstance(out);
  }

  @Override
  protected Operator createValue(IRInput in) throws IOException {
    Operator op = Operator.findOperatorInternal(in.readUTF());
    return op.readInstance(in);
  }
  @Override
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('O');
  }
  @Override
  public IRType<Operator> readType(IRInput in) { return this; }
    
  @Override
  public Operator fromString(String s) {
    return Operator.findOperatorInternal(s);
  }

  @Override
  public String toString(Operator op) {
    return op.name();
  }
}
