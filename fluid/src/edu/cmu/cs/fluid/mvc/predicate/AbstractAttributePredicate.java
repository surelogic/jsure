// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/AbstractAttributePredicate.java,v 1.6 2005/05/25 15:52:04 chance Exp $
package edu.cmu.cs.fluid.mvc.predicate;

import edu.cmu.cs.fluid.FluidError;

public abstract class AbstractAttributePredicate 
  implements AttributePredicate {
  @Override
  public String toString() {
    return getLabel();
  }
  @Override
  public boolean includesValues( Object[] values ) {
    if (values == null || values.length != 1) {
      throw new FluidError("Not a single value");
    }
    return includesValue(values[0]);
  }
}
