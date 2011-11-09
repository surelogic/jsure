// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/PickledAttributeModelStateType.java,v 1.8 2007/05/30 20:35:18 chance Exp $
package edu.cmu.cs.fluid.mvc.attr;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * The type of slots storing {@link PickledAttributeModelState}s.
 */

public class PickledAttributeModelStateType
implements IRType
{
  private static final char TYPE_ID = 'Q'; // FIX

  public static final PickledAttributeModelStateType prototype =
    new PickledAttributeModelStateType();



  //==============================================================
  //== Constructors / Initializers
  //==============================================================

  private PickledAttributeModelStateType()
  {
  }

  static
  {
    IRPersistent.registerIRType( prototype, TYPE_ID );
  }



  //==============================================================
  //== IRType Methods
  //==============================================================

  public boolean isValid( final Object x )
  {
    return x instanceof PickledAttributeModelState;
  }

  /** Type does not have an order */
  public Comparator getComparator() 
  {
    return null;
  }
  
  public void writeValue( final Object v, final IROutput out ) 
  throws IOException
  {
    final PickledAttributeModelState pickle = (PickledAttributeModelState)v;
    pickle.writeValue( out );
  }

  public Object readValue( final IRInput in )
  throws IOException
  {
    return PickledAttributeModelState.readValue( in );
  }

  public void writeType( final IROutput out )
  throws IOException
  {
    out.writeByte( TYPE_ID );
  }

  public IRType readType( final IRInput in )
  {
    return this;
  }

  public Object fromString( final String str )
  {
    throw new RuntimeException( "Method not yet implemented!" );
  }

  public String toString( final Object obj )
  {
    throw new RuntimeException( "Method not yet implemented!" );
  }
}

