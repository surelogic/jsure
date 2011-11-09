// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/TestVersionProjection.java,v 1.13 2003/07/15 18:39:11 thallora Exp $

package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.sequence.*;

/**
 * Program for testing VersionProjection models and VersionMarker models.
 */
public class TestVersionProjection
{
  public static void main( final String[] args )
  throws Exception
  {
    /* 
     * Create a versioned, labeled sequence
     */
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence", VersionedSlotFactory.prototype );

    /*
     * Set up the version space.
     *
     * >>> Version v1 is an empty sequence. <<<
     */
    Version.setVersion( Version.getInitialVersion() );
    Version.bumpVersion();
    final Version v1 = Version.getVersion();
    
    /*
     * Create v1 -> v2
     *
     * >>> Version v2 is [one, two, three] <<<
     */

    final IRNode node1 = new PlainIRNode();
    seq.appendElement( node1 );
    seq.setLabel( node1, "one" );
    final IRNode node2 = new PlainIRNode();
    seq.appendElement( node2 );
    seq.setLabel( node2, "two" );
    final IRNode node3 = new PlainIRNode();
    seq.appendElement( node3 );
    seq.setLabel( node3, "three" );

    final Version v2 = Version.getVersion();
    
    /*
     * Create v1 -> v2 -> v3
     *
     * >>> Version v3 is [eine, zwei, drei] <<<
     * >>> Version v3 has the same nodes as v2 <<<
     */

    seq.setLabel( node1, "eine" );
    seq.setLabel( node2, "zwei" );
    seq.setLabel( node3, "drei" );

    final Version v3 = Version.getVersion();
    
    /*
     * Create v1 -> v2 -> v3 -> v4
     *
     * >>> Version v4 is [eine, zwei, drei, vier] <<<
     * >>> Version v4 has the same nodes as v2 & v3, plus one more <<<
     */

    final IRNode node4a = new PlainIRNode();
    seq.appendElement( node4a );
    seq.setLabel( node4a, "vier" );

    final Version v4 = Version.getVersion();
    
    /*
     * Create v1 -> v2 -> v3 -> v4
     *              |
     *              +-> v5 
     *
     * >>> Version v5 is [one, two, three, four] <<<
     * >>> Version v5 has the same nodes as v2 & v3, plus one more <<<
     * >>> Node "four" is not the same as node "vier"
     */

    Version.setVersion( v2 );

    final IRNode node4b = new PlainIRNode();
    seq.appendElement( node4b );
    seq.setLabel( node4b, "four" );

    final Version v5 = Version.getVersion();
    
    /*
     * Create v1 -> v2 -> v3 -> v4
     *              |
     *              +-> v5 -> v6
     *
     * >>> Version v6 is [1, 2, 3, 4] <<<
     * >>> Version v6 has nodes distinct from the other versions <<<
     */
    
    final IRNode node1c = new PlainIRNode();
    seq.setElementAt( node1c, seq.location( 0 ) );
    seq.setLabel( node1c, "1" );
    final IRNode node2c = new PlainIRNode();
    seq.setElementAt( node2c, seq.location( 1 ) );
    seq.setLabel( node2c, "2" );
    final IRNode node3c = new PlainIRNode();
    seq.setElementAt( node3c, seq.location( 2 ) );
    seq.setLabel( node3c, "3" );
    final IRNode node4c = new PlainIRNode();
    seq.setElementAt( node4c, seq.location( 3 ) );
    seq.setLabel( node4c, "4" );

    final Version v6 = Version.getVersion();

    // ===================================================

/*
 * Keep this for the moment --- move to TestVersionSpace.java later
 *
    System.out.println( "******************* The Version Space is: " );
    vsDumper.dumpModel();
    System.out.println( "******************* End Version Space" );

    
    System.out.println( "******************* The REAL Version Space is: " );
    try {
      ForestModel realVersionSpace = PureForest.newForest( (fluid.tree.Tree)Version.getShadowTree(), Version.getInitialVersion().getShadowNode(), new Object(), "REAL VERSION SPACE", SimpleSlotFactory.prototype );
      ForestDumper vsd = new ForestDumper( realVersionSpace, System.out, false );
      System.out.println( "*** Real Version Space ***" );
      vsd.dumpModel();
    } catch( Exception e ) {
      System.err.println( "D'oh!" );
    }
    System.out.println( "******************* End REAL Version Space" );



    System.out.println( "******************* The REAL Version Space as tree is: " );
    try {
      ForestModel realVersionSpace = PureForest.newTree( (fluid.tree.Tree)Version.getShadowTree(), new Object(), "REAL VERSION SPACE 2", SimpleSlotFactory.prototype );
      realVersionSpace.addRoot( Version.getInitialVersion().getShadowNode() );
      ForestDumper vsd = new ForestDumper( realVersionSpace, System.out, false );
      System.out.println( "*** Real Version Space ***" );
      vsd.dumpModel();
    } catch( Exception e ) {
      System.err.println( "D'oh!" );
    }
    System.out.println( "******************* End REAL Version Space as tree " );
*/
    
    // ===================================================

    /*
     * Create the version marker
     */
    final VersionTrackerModel tracker = 
      VersionMarkerFactory.prototype.create( "Version Marker", v1 );

    /*
     * Create the version projection model.
     */
    final Model projection =
      FixedVersionProjectionFactory.prototype.create( "Projection", seq, tracker );
    final ModelDumper pDumper =
      new ModelDumper( projection, System.out, true );
    
    System.out.println( "== List should be [1,2,3,4] ==" );
    tracker.setVersion( v6 );
    pDumper.waitForBreak();

    System.out.println( "== List should be [eine,zwei,drei,vier] ==" );
    tracker.setVersion( v4 );
    pDumper.waitForBreak();

    System.out.println( "== List should be [one,two,three,four] ==" );
    tracker.setVersion( v5 );
    pDumper.waitForBreak();

    System.out.println( "== List should be [eine,zwei,drei] ==" );
    tracker.setVersion( v3 );
    pDumper.waitForBreak();

    System.out.println( "== List should be [] ==" );
    tracker.setVersion( v1 );
    pDumper.waitForBreak();

    System.out.println( "== List should be [one,two,three] ==" );
    tracker.setVersion( v2 );
    pDumper.waitForBreak();

    System.out.print( "== done ==" );
    System.exit( 0 );
  }
}

