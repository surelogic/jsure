// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/render/IconType.java,v 1.7 2007/05/30 20:35:20 chance Exp $
package edu.cmu.cs.fluid.render;

import java.io.IOException;
import java.util.Comparator;

import javax.swing.Icon;

import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * The type of {@link javax.swing.Icon}s&mdash;<em>not fully implemented</em>.
 */

public class IconType implements IRType<Icon>
{
  public static final IconType prototype = new IconType();
  private static char regType = 'K';

  static {
    IRPersistent.registerIRType( prototype, regType );
  }

  private IconType()
  {
    super();
  }

  public boolean isValid( final Object x )
  {
    return x instanceof Icon;
  }

  public Comparator<Icon> getComparator() 
  {
    return null;
  }
  
  public void writeValue( final Icon v, final IROutput out) 
  throws IOException
  {
    // needs to be implemented
  }

  public Icon readValue( final IRInput in )
  throws IOException
  {
    // needs to be implemented
    return null;
  }

  public void writeType( final IROutput out )
  throws IOException
  {
    out.writeByte( regType );
  }

  public IRType<Icon> readType( final IRInput in )
  {
    return this;
  }

  public Icon fromString( final String str )
  {
    throw new RuntimeException( "Method not yet implemented!" );
  }

  public String toString( final Icon obj )
  {
    throw new RuntimeException( "Method not yet implemented!" );
  }
}
