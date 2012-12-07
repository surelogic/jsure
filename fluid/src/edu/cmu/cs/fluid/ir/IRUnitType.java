/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRUnitType.java,v 1.8 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

/** The only unit value is null.
 */
@ThreadSafe
public class IRUnitType implements IRType<Void>, Comparator<Void> {
  private IRUnitType() {}
  public static final IRUnitType prototype = new IRUnitType();
  static { IRPersistent.registerIRType(prototype,'U'); }
  public boolean isValid(Object x) {
    return x == null;
  }

  public int compare( final Void o1, final Void o2 )
  {
    if( (o1 == null) && (o2 == null) ) {
      return 0;
    } else {
      throw new ClassCastException();
    }
  }

  public Comparator<Void> getComparator() 
  {
    return this;
  }

  public void writeValue(Void v, IROutput out) {}
  public Void readValue(IRInput in){ return null; }
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('U');
  }
  public IRType<Void> readType(IRInput in) { return this; }

  /* only valid IRUnitType is null. and it is represented by
   * null string ("")
   */

  /** @return <code>null</code> */
  public Void fromString(String s) {
    return null;
  }

  /** @return if o is <code>null</code> then returns <code>""</code>,
              else returns <code>null</code> */
  public String toString(Void o) {
    if (o == null) {
      return "";
    } else {
      return null;
    }
  }
}
