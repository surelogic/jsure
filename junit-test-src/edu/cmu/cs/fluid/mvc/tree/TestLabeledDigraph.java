// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/TestLabeledDigraph.java,v 1.5 2005/07/25 17:17:57 aarong Exp $

package edu.cmu.cs.fluid.mvc.tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelDumper;
import edu.cmu.cs.fluid.mvc.ModelUtils;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

public class TestLabeledDigraph
{
  final Map<String,IRNode> labels;
  final BufferedReader input;
  final ModelDumper dumper;
  final LabeledDigraph digraph;
  
  private TestLabeledDigraph()
  throws SlotAlreadyRegisteredException
  {
    digraph =
      StandardLabeledDigraphFactory.mutablePrototype.create(
       "My Digraph", SimpleSlotFactory.prototype );
    dumper = new ModelDumper( digraph, System.out );
    input = new BufferedReader( new InputStreamReader( System.in ) );
    labels = new HashMap<String,IRNode>();
  }
  
  public static void main( final String[] args )
  throws Exception
  {
    final TestLabeledDigraph tester = new TestLabeledDigraph();
    tester.test();
  }
  
  private IRNode getNodeFromLabel( final String label ) 
  {
    return labels.get( label );
  }
  
  public void test()
  throws InterruptedException, IOException
  {    
    while( true ) {
      System.out.println( "quit, node <label>, edge <parent> <child>, remove <label>, nuke <label>" );
      System.out.print( "> " );
      
      final StringTokenizer st = new StringTokenizer( input.readLine() );
      final String cmd = st.nextToken().intern();

      if( cmd == "quit" ) {
        ModelUtils.shutdownChain(digraph);
        break;
      } else if( cmd == "node" ) { // add node to model
        final String label = st.nextToken();
        IRNode node = getNodeFromLabel(label);
        if( node == null ) node = new PlainIRNode();
        digraph.addNode( node, new AVPair[0] );
        dumper.waitForBreak();
        digraph.setLabel( node, label );
        dumper.waitForBreak();
        labels.put( label, node );
      } else if( cmd == "edge" ) { // add edge
        final String label1 = st.nextToken();
        final String label2 = st.nextToken();
        final IRNode pnode = getNodeFromLabel(label1);
        final IRNode cnode = getNodeFromLabel(label2);
        if( pnode == null ) {
          System.out.println( "Cannot find node: " + label1 );
        }
        if( cnode == null ) {
          System.out.println( "Cannot find node: " + label2 );
        }
        if( pnode != null && cnode != null ) {
          digraph.addChild( pnode, cnode );
          dumper.waitForBreak();
        }
      } else if( cmd == "remove" ) {
        final String label = st.nextToken();
        final IRNode node = getNodeFromLabel( label );
        if( node == null ) {
          System.out.println( "Cannot find node: " + label );
        } else {
          digraph.removeNode( node );
          dumper.waitForBreak();
        }
      } else if( cmd == "nuke" ) {
//        final String label = st.nextToken();
//        final IRNode node = getNodeFromLabel( label );
//        if( node == null ) {
//          System.out.println( "Cannot find node: " + label );
//        } else {
//          digraph.removeEdges( node );
//          dumper.waitForBreak();
//        }
      }
    }
  }
}
