/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/AttributePredicateType.java,v 1.9 2007/05/30 20:35:18 chance Exp $ */
package edu.cmu.cs.fluid.mvc.predicate;

import java.io.IOException;
import java.util.Comparator;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * The type of {@link AttributePredicate}s&mdash;<em>not fully implemented</em>.
 */
public class AttributePredicateType implements IRType<AttributePredicate> {

  public static final AttributePredicateType prototype = new AttributePredicateType();
  static {
    IRPersistent.registerIRType(prototype, 'P');
  }

  private AttributePredicateType() {
    super();
  }

  public boolean isValid(final Object x) {
    return x instanceof AttributePredicate;
  }

  public Comparator<AttributePredicate> getComparator() {
    return null;
  }

  public void writeValue(final AttributePredicate v, final IROutput out) throws IOException {
    // needs to be implemented
  }

  public AttributePredicate readValue(final IRInput in) throws IOException {
    // needs to be implemented
    return null;
  }

  public void writeType(final IROutput out) throws IOException {
    out.writeByte('P');
  }

  public IRType<AttributePredicate> readType(final IRInput in) {
    return this;
  }

  public AttributePredicate fromString(final String str) {
    throw new RuntimeException("Method not yet implemented!");
  }

  public String toString(final AttributePredicate obj) {
    throw new RuntimeException("Method not yet implemented!");
  }
}
