// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/TestLabeledForest.java,v 1.9 2005/07/01 16:15:36 chance Exp $

package edu.cmu.cs.fluid.mvc.tree;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;

public class TestLabeledForest
{
  public static void main( final String[] args )
  throws Exception
  {
    final LabeledForest forest =
      StandardLabeledForestFactory.mutablePrototype.create(
       "My Forest", SimpleSlotFactory.prototype );

    final ForestDumper dumper = new ForestDumper( forest, System.out );

    final BufferedReader input =
      new BufferedReader( new InputStreamReader( System.in ) );

    final Map<String,IRNode> labels = new HashMap<String,IRNode>();
    while( true ) {
      System.out.println( "quit, root <label>, node <parent> <label>" );
      System.out.print( "> " );
      
      final StringTokenizer st = new StringTokenizer( input.readLine() );
      final String cmd = st.nextToken().intern();

      if( cmd == "quit" ) {
        System.exit( 0 );
      } else if( cmd == "root" ) { // append root: "r <label>"
        final String label = st.nextToken();
        final IRNode node = new PlainIRNode();
        forest.initNode( node );
        forest.addRoot( node );
        dumper.waitForBreak();
        forest.setLabel( node, label );
        dumper.waitForBreak();
        labels.put( label, node );
      } else if( cmd == "node" ) { // add child: "n <parent label> <child label>"
        final String label1 = st.nextToken();
        final String label2 = st.nextToken();
        final IRNode parent = labels.get( label1 );
        final IRNode node = new PlainIRNode();
        forest.initNode( node );
        forest.appendSubtree( parent, node );
        dumper.waitForBreak();
        forest.setLabel( node, label2 );
        dumper.waitForBreak();
        labels.put( label2, node );
      }
    }
  }
}
