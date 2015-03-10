// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/ForestDumper.java,v 1.11 2007/07/10 22:16:34 aarong Exp $

package edu.cmu.cs.fluid.mvc.tree;

import java.io.PrintStream;
import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.ModelDumper;
import edu.cmu.cs.fluid.ir.IRNode;

public class ForestDumper
extends ModelDumper
{
  public ForestDumper( final ForestModel mod, final PrintStream w )
  {
    super( mod, w );
  }

  public ForestDumper( final ForestModel mod, final PrintStream w,
		       final boolean add )
  {
    super( mod, w, add );
  }

  @Override
  protected void dumpModelStructure()
  {
    final ForestModel forest = (ForestModel)model;
    final Iterator<IRNode> roots = forest.getRoots();
    int idx = 0;
    while( roots.hasNext() ) {
      final IRNode root = roots.next();
      dumpNode( forest, root, "", null, idx );
      idx += 1;
    }
  }

  private void dumpNode( final ForestModel forest, final IRNode node,
                         final String prefix, final IRNode parent,
			 final int idx )
  {
    writer.println( prefix + "*** Child #" + idx + " of " +
                    ((parent == null) 
		     ? "Root"
		     : forest.idNode( parent )) + " ***" );
    writer.println( prefix + "*** Node = " + forest.idNode( node ) );
    dumpNodeAttributes( node, prefix );

    final Iterator<IRNode> children = forest.children( node );
    int childIdx = 0;
    while( children.hasNext() ) {
      final IRNode child = children.next();
      dumpNode( forest, child, (prefix + "  "), node, childIdx );
      childIdx += 1;
    }
  }
}
