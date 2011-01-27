/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/TestIdentityVisibilityView.java,v 1.8 2005/07/01 16:15:37 chance Exp $
 *
 * TestIdentityVisibilityView.java
 * Created on March 15, 2002, 5:21 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelDumper;
import edu.cmu.cs.fluid.mvc.set.LabeledSet;
import edu.cmu.cs.fluid.mvc.set.LabeledSetFactory;
import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;

/**
 * Program to test IdentityVisibilityView implemenations.
 *
 * <p>Executes additions and removals on a source SetModel.  The
 * VisibilityModel should grow and shrink with the modifications, and
 * IS_VISIBLE should always be true for all nodes in the model.
 *
 * @author Aaron Greenhouse
 */
public class TestIdentityVisibilityView
{
  private static Map<String,IRNode> elts = new HashMap<String,IRNode>();

  public static void main( final String[] args )
  throws Exception
  {
    final SetModel set =
      LabeledSetFactory.prototype.create(
        "test set", SimpleExplicitSlotFactory.prototype );

    final VisibilityModel visModel = 
      IdentityVisibilityViewFactory.prototype.create( "Visibility View", set );
    
    final ModelDumper dumper = new ModelDumper( visModel, System.out );

    int count = 0;
    while( count < args.length ) {
      count = processCmd( set, args, count );
      dumper.waitForBreak();
    }
    System.out.flush();
  }

  private static int processCmd( final SetModel set,
                                 final String[] args,
                                 int current )
  throws NumberFormatException
  {
    final String cmd = args[current++].intern();

    if( cmd == "a" ) { // add node: "a <label>"
      final String label = args[current++];
      System.out.println( "**** Add node \"" + label + "\" ****" );
      final IRNode node = new PlainIRNode();
      if( !elts.keySet().contains( label ) ) {
        elts.put( label, node );
        set.addNode( node, new AVPair[] { new AVPair( LabeledSet.LABEL, label ) } );
      }
    } else if( cmd == "r" ) { // remove node "r <label>"
      final String label = args[current++];
      System.out.println( "**** Remove node \"" + label + "\" ****" );
      final IRNode node = elts.get( label );
      if( node != null ) {
        elts.remove( label );
        set.removeNode( node );
      }
    }
    return current;
  }
}
