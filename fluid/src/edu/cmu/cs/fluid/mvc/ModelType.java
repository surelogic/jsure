/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ModelType.java,v 1.9 2007/05/30 20:35:19 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.io.IOException;
import java.util.Comparator;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * The type of {@link Model}s&mdash;<em>not fully implemented</em>.
 */
public class ModelType implements IRType<Model> {

  public static final ModelType prototype = new ModelType();
  static {
    IRPersistent.registerIRType(prototype, 'M');
  }

  private ModelType() {
    super();
  }

  @Override
  public boolean isValid(final Object x) {
    return x instanceof Model;
  }

  @Override
  public Comparator<Model> getComparator() {
    throw new RuntimeException("Method not yet implemented!");
  }

  @Override
  public void writeValue(final Model v, final IROutput out) throws IOException {
    throw new RuntimeException("Method not yet implemented!");
  }

  @Override
  public Model readValue(final IRInput in) throws IOException {
    throw new RuntimeException("Method not yet implemented!");
  }

  @Override
  public void writeType(final IROutput out) throws IOException {
    out.writeByte('M');
  }

  @Override
  public IRType<Model> readType(final IRInput in) {
    return this;
  }

  @Override
  public Model fromString(final String str) {
    throw new RuntimeException("Method not yet implemented!");
  }

  @Override
  public String toString(final Model obj) {
    throw new RuntimeException("Method not yet implemented!");
  }
}
