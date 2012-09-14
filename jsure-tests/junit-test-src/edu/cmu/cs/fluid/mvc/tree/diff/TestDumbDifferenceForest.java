// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/diff/TestDumbDifferenceForest.java,v 1.7 2006/03/29 20:08:42 chance Exp $

package edu.cmu.cs.fluid.mvc.tree.diff;

import edu.cmu.cs.fluid.mvc.tree.ForestDumper;
import edu.cmu.cs.fluid.mvc.tree.LabeledForest;
import edu.cmu.cs.fluid.mvc.tree.StandardLabeledForestFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;

public class TestDumbDifferenceForest
{
  @SuppressWarnings("unchecked")
public static void main( final String[] args )
  throws Exception
  {
    /* 
     * Create the input forests
     */
    final LabeledForest baseForest =
      StandardLabeledForestFactory.mutablePrototype.create(
       "Base Forest", SimpleSlotFactory.prototype );
    final SlotInfo<String> baseLabels = baseForest.getNodeAttribute( LabeledForest.LABEL );

    final LabeledForest deltaForest =
      StandardLabeledForestFactory.mutablePrototype.create(
       "Delta Forest", SimpleSlotFactory.prototype );
    final SlotInfo<String> deltaLabels = deltaForest.getNodeAttribute( LabeledForest.LABEL );

    /* Base:
     *  n1 ("root")
     *    n2 ("a")
     *    n3 ("b")
     *      n4 ("c");
     *    n5 ("d")
     *      n6 ("e")
     *        n7 ("f")
     *
     * Delta:
     *  
     *  n1 ("root")
     *    n2 ("a");
     *    n3 ("b")
     *      n4 ("1")
     *      n10 ("abc")
     *      n7 ("f");
     *    n5 ("d");
     */

    /*
     * create all the nodes.
     */
    final IRNode n1 = new PlainIRNode();
    final IRNode n2 = new PlainIRNode();
    final IRNode n3 = new PlainIRNode();
    final IRNode n4 = new PlainIRNode();
    final IRNode n5 = new PlainIRNode();
    final IRNode n6 = new PlainIRNode();
    final IRNode n7 = new PlainIRNode();
    final IRNode n10 = new PlainIRNode();

    /*
     * Build the base forest.
     */
    baseForest.initNode( n1 );
    baseForest.addRoot( n1 );
    baseForest.initNode( n2 );
    baseForest.appendSubtree( n1, n2 );
    baseForest.initNode( n3 );
    baseForest.appendSubtree( n1, n3 );
    baseForest.initNode( n4 );
    baseForest.appendSubtree( n3, n4 );
    baseForest.initNode( n5 );
    baseForest.appendSubtree( n1, n5 );
    baseForest.initNode( n6 );
    baseForest.appendSubtree( n5, n6 );
    baseForest.initNode( n7 );
    baseForest.appendSubtree( n6, n7 );
    
    n1.setSlotValue( baseLabels, "root" );
    n2.setSlotValue( baseLabels, "a" );
    n3.setSlotValue( baseLabels, "b" );
    n4.setSlotValue( baseLabels, "c" );
    n5.setSlotValue( baseLabels, "d" );
    n6.setSlotValue( baseLabels, "e" );
    n7.setSlotValue( baseLabels, "f" );

    /*
     * build the delta forest.
     */
    deltaForest.initNode( n1 );
    deltaForest.addRoot( n1 );
    deltaForest.initNode( n2 );
    deltaForest.appendSubtree( n1, n2 );
    deltaForest.initNode( n3 );
    deltaForest.appendSubtree( n1, n3 );
    deltaForest.initNode( n4 );
    deltaForest.appendSubtree( n3, n4 );
    deltaForest.initNode( n10 );
    deltaForest.appendSubtree( n3, n10 );
    deltaForest.initNode( n7 );
    deltaForest.appendSubtree( n3, n7 );
    deltaForest.initNode( n5 );
    deltaForest.appendSubtree( n1, n5 );
    
    n1.setSlotValue( deltaLabels, "root" );
    n2.setSlotValue( deltaLabels, "a" );
    n3.setSlotValue( deltaLabels, "b" );
    n4.setSlotValue( deltaLabels, "1" );
    n5.setSlotValue( deltaLabels, "d" );
    n7.setSlotValue( deltaLabels, "f" );
    n10.setSlotValue( deltaLabels, "abc" );

    // ===================================================

    /*
     * Create the difference model.
     */
    final DumbDifferenceForest diff =
      DumbDifferenceForestFactory.prototype.create(
        "Diff", baseForest, deltaForest );

    final ForestDumper dumper = new ForestDumper( diff, System.out, true );

    dumper.dumpModel();
    System.out.print( "== done ==" );
    System.exit( 0 );
  }
}
