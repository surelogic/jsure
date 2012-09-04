/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/TestPredicateBasedVisibilityView.java,v 1.8 2007/06/04 16:55:01 aarong Exp $
 *
 * TestPredicateBasedVisibilityView.java
 * Created on March 21, 2002, 10:57 AM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelDumper;
import edu.cmu.cs.fluid.mvc.SimpleModelRenderer;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModelCore;
import edu.cmu.cs.fluid.mvc.predicate.SimplePredicateViewFactory;
import edu.cmu.cs.fluid.mvc.sequence.LabeledSequence;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModel;
import edu.cmu.cs.fluid.mvc.sequence.SimpleLabeledSequenceFactory;
import edu.cmu.cs.fluid.ir.*;

/**
 * Program to test {@link PredicateBasedVisibilityView}s.
 *
 * @author Aaron Greenhouse
 */
public class TestPredicateBasedVisibilityView
{
  @SuppressWarnings("unchecked")
  public static void main( final String[] args )
  throws Exception
  {
    /**
     * Model from which we will obtain A predicate models, and 
     * for which the visibility will be computed.
     */
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence", SimpleSlotFactory.prototype );
    seq.addNode( new PlainIRNode(),
                 new AVPair[] { new AVPair( LabeledSequence.LABEL,
                                            ">>>ONE<<<" ) } );
    seq.addNode( new PlainIRNode(),
                 new AVPair[] { new AVPair( LabeledSequence.LABEL,
                                            ">>>TWO<<<" ) } );
    seq.addNode( new PlainIRNode(),
                 new AVPair[] { new AVPair( LabeledSequence.LABEL,
                                            ">>>THREE<<<" ) } );
    seq.addNode( new PlainIRNode(),
                 new AVPair[] { new AVPair( LabeledSequence.LABEL,
                                            ">>>FOUR<<<" ) } );
    seq.addNode( new PlainIRNode(),
                 new AVPair[] { new AVPair( LabeledSequence.LABEL,
                                            ">>>FIVE<<<" ) } );
    
    /* Init PredicateModel */
    final PredicateModel predModel = 
      SimplePredicateViewFactory.prototype.create( "Predicate Model", seq );

    /* Init VisibilityModel */
    final PredicateBasedVisibilityView vizModel =
      PredicateBasedVisibilityViewFactory.prototype.create(
        "Viz Model", seq, predModel );
    
    /* Init model renderers */
    final JFrame srcRenderer = new SimpleModelRenderer( seq );
    final JFrame predRenderer = new SimpleModelRenderer( predModel );
    final JFrame vizRenderer = new SimpleModelRenderer( vizModel );
    srcRenderer.setVisible( true );
    predRenderer.setVisible( true );
    vizRenderer.setVisible( true );
    
    /* Init model dumper */
    final ModelDumper dumper = new ModelDumper( vizModel, System.out );
    dumper.setName( "Visibility Dumper" );

    final BufferedReader input =
      new BufferedReader( new InputStreamReader( System.in ) );

    while( true ) {
      System.out.println(   "quit, "
                          + "help, "
                          + "set <idx> <isVisible>, "
                          + "move <oldIdx> <newIdx>, "
                          + "viz [true|false], "
                          + "cviz [true|false]" );
      System.out.print( "> " );
      System.out.flush();

      final StringTokenizer st = new StringTokenizer( input.readLine() );
      final String cmd = st.nextToken().intern();

      if( cmd == "quit" ) {
        System.exit( 0 );
      } else if( cmd == "help" ) {
        System.out.println( "Default visibility = " + vizModel.getDefaultVisibility() );
        System.out.println( "The predicate model state is: " );
        int c = 0;
        for( Iterator<IRNode> i = predModel.getNodes(); i.hasNext(); c += 1 ) {
          final IRNode n = i.next();
          System.out.println(
              "  " + c + " = " + seq.getAttributeName( predModel.getAttributeNode(n) )
            + "::" + predModel.getPredicate( n ).getLabel()
            + " [" + predModel.isVisible( n ) + "]" );
        }
        System.out.println();
      } else if( cmd == "cviz" ) {
        final boolean defaultViz = st.nextToken().intern() == "true";
        vizModel.setDefaultVisibility( defaultViz );
        dumper.waitForBreak();
      } else if( cmd == "viz" ) {
        final boolean defaultViz = st.nextToken().intern() == "true";
        vizModel.getCompAttribute(
          PredicateBasedVisibilityView.DEFAULT_VISIBILITY ).setValue( 
            defaultViz ? Boolean.TRUE : Boolean.FALSE );
        dumper.waitForBreak();
      } else if( cmd == "set" ) {
        final int loc = Integer.parseInt( st.nextToken() );
        final IRLocation irloc = predModel.location( loc );
        final int isVisible = Integer.parseInt( st.nextToken() );
        final IRNode node = predModel.elementAt( irloc );
        predModel.setNodeAttributes(
          node,
          new AVPair[] {
                new AVPair( PredicateModel.IS_VISIBLE,
                            PredicateModelCore.getVisibleEnum().getElement(
                              isVisible ) ) } );
        dumper.waitForBreak();
      } else if( cmd == "move" ) {
        final int loc1 = Integer.parseInt( st.nextToken() );
        final IRLocation irloc1 = predModel.location( loc1 );
        final int loc2 = Integer.parseInt( st.nextToken() );

        final IRNode node = predModel.elementAt( irloc1 );
        final SlotInfo<Integer> si = predModel.getNodeAttribute( SequenceModel.INDEX );
        node.setSlotValue( si, Integer.valueOf(loc2) );
        dumper.waitForBreak();
      }
    }
  }
}
