// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/EnumerationAlreadyDefinedException.java,v 1.4 2003/07/02 20:19:15 thallora Exp $

package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidException;

public class EnumerationAlreadyDefinedException
extends FluidException
{
  public EnumerationAlreadyDefinedException( final String name )
  {
    super( "An IREnumrationType of name \"" + name + "\" is already defined." );
  }
}
