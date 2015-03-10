/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/ForestEllipsisPolicyType.java,v 1.9 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * The type of {@link ForestEllipsisPolicy}s&mdash;
 * <em>not fully implemented</em>.
 */

public class ForestEllipsisPolicyType implements IRType<ForestEllipsisPolicy>
{
  public static final ForestEllipsisPolicyType prototype = new ForestEllipsisPolicyType();

  static {
    IRPersistent.registerIRType( prototype, '$' );
  }

  private ForestEllipsisPolicyType()
  {
    super();
  }

  @Override
  public boolean isValid( final Object x )
  {
    return x instanceof ForestEllipsisPolicy;
  }

  @Override
  public Comparator<ForestEllipsisPolicy> getComparator() 
  {
    return null;
  }
  
  @Override
  public void writeValue( final ForestEllipsisPolicy v, final IROutput out ) 
  throws IOException
  {
    // needs to be implemented
  }

  @Override
  public ForestEllipsisPolicy readValue( final IRInput in )
  throws IOException
  {
    // needs to be implemented
    return null;
  }

  @Override
  public void writeType( final IROutput out )
  throws IOException
  {
    out.writeByte( 'Q' );
  }

  @Override
  public IRType<ForestEllipsisPolicy> readType( final IRInput in )
  {
    return this;
  }

  @Override
  public ForestEllipsisPolicy fromString(String s) {
    throw new NotImplemented();
  }

  @Override
  public String toString(ForestEllipsisPolicy o) {
    throw new NotImplemented();
  }
}
