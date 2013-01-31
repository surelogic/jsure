// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/TemplateType.java,v 1.7 2006/03/28 20:58:45 chance Exp $
package edu.cmu.cs.fluid.template;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRType;

@SuppressWarnings("deprecation")
public class TemplateType implements IRType {
  public static final TemplateType prototype = new TemplateType();
  public static final TemplateType t = prototype;

  private TemplateType()
  {
    super();
  }

  @Override
  public boolean isValid(Object value) {
    return (value instanceof Template);
  }

  @Override
  public Comparator getComparator() 
  {
    return null;
  }
  
  /** Write a value out. */
  @Override
  public void writeValue(Object value, IROutput out) throws IOException {
  }
  
  /** Read a value in. */
  @Override
  public Object readValue(IRInput in) throws IOException {
    return null;
  }

  /** Write the type out (starting with a registered byte). */
  @Override
  public void writeType(IROutput out) throws IOException {
  }

  /** Read a type in continuing after the registered byte. */
  @Override
  public IRType readType(IRInput in) throws IOException {
    return null;
  }

  /** @exception fluid.NotImplemented */
  @Override
  public Object fromString(String s) {
    throw new NotImplemented("fluid.ir.TemplateType.fromString()");
  }

  /** @exception fluid.NotImplemented */
  @Override
  public String toString(Object o) {
    throw new NotImplemented("fluid.ir.TemplateType.toString()");
  }

}
