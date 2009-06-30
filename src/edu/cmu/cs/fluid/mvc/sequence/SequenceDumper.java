// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SequenceDumper.java,v 1.11 2007/07/05 18:15:14 aarong Exp $
package edu.cmu.cs.fluid.mvc.sequence;

import java.io.PrintStream;
import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.ModelDumper;
import edu.cmu.cs.fluid.ir.IRNode;

public class SequenceDumper
extends ModelDumper
{
  public SequenceDumper( final SequenceModel mod, final PrintStream w )
  {
    super( mod, w );
  }

  public SequenceDumper( final SequenceModel mod, final PrintStream w,
                         final boolean add )
  {
    super( mod, w, add );
  }

  @Override
  protected void dumpModelStructure()
  {
    int i = 0;
    final Iterator nodes = model.getNodes();
    while( nodes.hasNext() ) {
      final IRNode node = (IRNode)nodes.next();
      writer.println( "*** At Sequence Index " + (i++) + " ***" );
      writer.println( "Node: " + node );
      dumpNodeAttributes( node );
    } 
  }
}

