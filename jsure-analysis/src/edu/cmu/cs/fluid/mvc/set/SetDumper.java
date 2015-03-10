// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/SetDumper.java,v 1.9 2003/07/15 18:39:12 thallora Exp $

package edu.cmu.cs.fluid.mvc.set;

import java.io.PrintStream;

import edu.cmu.cs.fluid.mvc.ModelDumper;

public class SetDumper
extends ModelDumper
{
  public SetDumper( final SetModel mod, final PrintStream w )
  {
    super( mod, w );
  }

  public SetDumper( final SetModel mod, final PrintStream w, final boolean add )
  {
    super( mod, w, add );
  }
}

