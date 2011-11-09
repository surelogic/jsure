/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SequenceEllipsisPolicyType.java,v 1.9 2007/05/30 20:35:18 chance Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * The type of {@link SequenceEllipsisPolicy}s&mdash;
 * <em>not fully implemented</em>.
 */

public class SequenceEllipsisPolicyType implements IRType<SequenceEllipsisPolicy>
{
  public static final SequenceEllipsisPolicyType prototype = new SequenceEllipsisPolicyType();

  static {
    IRPersistent.registerIRType( prototype, 'Q' );
  }

  private SequenceEllipsisPolicyType()
  {
    super();
  }

  public boolean isValid( final Object x )
  {
    return x instanceof SequenceEllipsisPolicyType;
  }

  public Comparator<SequenceEllipsisPolicy> getComparator() 
  {
    return null;
  }
  
  public void writeValue( final SequenceEllipsisPolicy v, final IROutput out ) 
  throws IOException
  {
    // needs to be implemented
  }

  public SequenceEllipsisPolicy readValue( final IRInput in )
  throws IOException
  {
    // needs to be implemented
    return null;
  }

  public void writeType( final IROutput out )
  throws IOException
  {
    out.writeByte( 'Q' );
  }

  public IRType<SequenceEllipsisPolicy> readType( final IRInput in )
  {
    return this;
  }

  public SequenceEllipsisPolicy fromString( final String str )
  {
    throw new RuntimeException( "Method not yet implemented!" );
  }

  public String toString( final SequenceEllipsisPolicy obj )
  {
    throw new RuntimeException( "Method not yet implemented!" );
  }
}
