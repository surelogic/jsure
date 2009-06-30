// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/tree/TestVersionedForestProjection.java,v 1.11 2006/03/30 16:20:27 chance Exp $

package edu.cmu.cs.fluid.mvc.version.tree;

import edu.cmu.cs.fluid.mvc.tree.ForestDumper;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.tree.LabeledForest;
import edu.cmu.cs.fluid.mvc.tree.StandardLabeledForestFactory;
import edu.cmu.cs.fluid.mvc.version.VersionMarkerFactory;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;

public class TestVersionedForestProjection
{
  @SuppressWarnings("unchecked")
  public static void main( final String[] args )
  throws Exception
  {
    /* 
     * Create a versioned, labeled forest.
     */
    final LabeledForest forest =
      StandardLabeledForestFactory.mutablePrototype.create(
       "My Forest", VersionedSlotFactory.prototype );
    final SlotInfo<String> labels = forest.getNodeAttribute( LabeledForest.LABEL );

    /*
     * Set up the version space.
     *
     * >>> Version v1 is an empty forest. <<<
     */
    Version.setVersion( Version.getInitialVersion() );
    Version.bumpVersion();
    final Version v1 = Version.getVersion();

    /*
     * Create v1 -> v2
     *
     * >>> Version v2 is [A [B [D]] [C [E]]]  <<<
     */
    
    final IRNode nodeA = new PlainIRNode();
    forest.initNode( nodeA );
    forest.addRoot( nodeA );
    final IRNode nodeB = new PlainIRNode();
    forest.initNode( nodeB );
    forest.appendSubtree( nodeA, nodeB );
    final IRNode nodeC = new PlainIRNode();
    forest.initNode( nodeC );
    forest.appendSubtree( nodeA, nodeC );
    final IRNode nodeD = new PlainIRNode();
    forest.initNode( nodeD );
    forest.appendSubtree( nodeB, nodeD );
    final IRNode nodeE = new PlainIRNode();
    forest.initNode( nodeE );
    forest.appendSubtree( nodeC, nodeE );
    
    nodeA.setSlotValue( labels, "A" );
    nodeB.setSlotValue( labels, "B" );
    nodeC.setSlotValue( labels, "C" );
    nodeD.setSlotValue( labels, "D" );
    nodeE.setSlotValue( labels, "E" );

    final Version v2 = Version.getVersion();

    /*
     * Create v1 -> v2 -> v3
     *
     * >>> Version v3 is [A [B [D]] [C [E]]]  [1 [2 3]] <<<
     */

    final IRNode node1 = new PlainIRNode();
    final IRNode node2 = new PlainIRNode();
    final IRNode node3 = new PlainIRNode();
    forest.initNode( node1 );
    forest.initNode( node2 );
    forest.initNode( node3 );
    forest.addRoot( node1 );
    forest.appendSubtree( node1, node2 );
    forest.appendSubtree( node1, node3 );

    node1.setSlotValue( labels, "1" );
    node2.setSlotValue( labels, "2" );
    node3.setSlotValue( labels, "3" );

    final Version v3 = Version.getVersion();

    /*
     * Create v1 -> v2 -> v3
     *              |
     *              +-> v4
     *
     * >>> Version v4 is [A [F]] <<<
     */

    Version.setVersion( v2 );

    final IRNode nodeF = new PlainIRNode();
    forest.clearParent( nodeB );
    forest.clearParent( nodeC );
    forest.initNode( nodeF );
    forest.appendSubtree( nodeA, nodeF );
    nodeF.setSlotValue( labels, "F" );

    final Version v4 = Version.getVersion();

    /*
     * Create v1 -> v2 -------> v3
     *              |           |
     *              +-> v4      +-> v5
     *
     * >>> Version v5 is [one [two three]]  [A [B [D]] [C [E]]] <<<
     * >>> Same nodes as v3 <<<
     */

    Version.setVersion( v3 );

    node1.setSlotValue( labels, "one" );
    node2.setSlotValue( labels, "two" );
    node3.setSlotValue( labels, "three" );

    final IRSequence<IRNode> roots = 
      (IRSequence)forest.getCompAttribute( ForestModel.ROOTS ).getValue();
    final IRNode secondRoot = roots.elementAt( 1 );
    roots.removeElementAt( roots.location( 1 ) );
    roots.insertElement( secondRoot );

    final Version v5 = Version.getVersion();

    // ===================================================

    /*
     * Create the version marker
     */
    final VersionTrackerModel tracker = 
      VersionMarkerFactory.prototype.create( "Version Marker", v1 );

    /*
     * Create the version projection model.
     */
    final ForestModel projection =
      FixedVersionForestProjectionFactory.prototype.create(
        "Projection", forest, tracker );

    final ForestDumper pDumper =
      new ForestDumper( projection, System.out, true );
    
    System.out.println( "== Forest should be [a [f]] ==" );
    tracker.setVersion( v4 );
    pDumper.waitForBreak();

    System.out.println( "== Forest should be empty ==" );
    tracker.setVersion( v1 );
    pDumper.waitForBreak();

    System.out.println( "== Forest should be [one [two three]] [a [b [d]] [c [e]]] ==" );
    tracker.setVersion( v5 );
    pDumper.waitForBreak();

    System.out.println( "== Forest should be [a [b [d]] [c [e]]] ==" );
    tracker.setVersion( v2 );
    pDumper.waitForBreak();

    System.out.println( "== Forest should be [a [b [d]] [c [e]]] [1 [2 3]] ==" );
    tracker.setVersion( v3 );
    pDumper.waitForBreak();

    System.out.print( "== done ==" );
    System.exit( 0 );
  }
}

