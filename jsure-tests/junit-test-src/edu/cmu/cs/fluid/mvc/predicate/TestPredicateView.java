// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/TestPredicateView.java,v 1.14 2007/06/04 16:55:01 aarong Exp $

package edu.cmu.cs.fluid.mvc.predicate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.sequence.LabeledSequence;
import edu.cmu.cs.fluid.mvc.sequence.SequenceDumper;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModel;
import edu.cmu.cs.fluid.mvc.sequence.SimpleLabeledSequenceFactory;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;


public class TestPredicateView
{
  public static void main( final String[] args )
  throws Exception
  {
    // Init source sequence
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence", SimpleSlotFactory.prototype );

    // Init PredicateModel
    final PredicateModel predModel = 
      SimplePredicateViewFactory.prototype.create( "Predicate Model", seq );

    // Init sequence dumper
    final SequenceDumper dumper = new SequenceDumper( predModel, System.out );
    dumper.setName( "Predicate Dumper" );
    dumper.dumpModel( predModel );

    PickledPredicateModelState pickledState = null;

    final BufferedReader input =
      new BufferedReader( new InputStreamReader( System.in ) );

    while( true ) {
      System.out.println( "quit, save, restore, s <idx> <isVisible> <isStyled>, mi <oldIdx> <newIdx>" );
      System.out.print( "> " );
      System.out.flush();

      final String cmd = input.readLine().trim().intern();

      if( cmd == "quit" ) {
        System.exit( 0 );
      } if( cmd == "save" ) { // save state
        pickledState = predModel.getPickledState();
        System.out.println( "Pickle = " + pickledState );
        System.out.println();
      } else if( cmd == "restore" ) { // restore from pickle
        predModel.setStateFromPickle( pickledState );
        dumper.waitForBreak();
      } else {
	final StringTokenizer st = new StringTokenizer( cmd );
        final String cmd2 = st.nextToken().intern();
        
	if( st.hasMoreTokens() ) {
	  if( cmd2 == "s" ) {
            // set predicate flags "s <idx> <isVisible> <isStyled>"
            final int loc = Integer.parseInt( st.nextToken() );
            final IRLocation irloc = predModel.location( loc );
            final int isVisible = Integer.parseInt( st.nextToken() );
            final boolean isStyled = st.nextToken().intern() == "true";
            final IRNode node = predModel.elementAt( irloc );
            predModel.setNodeAttributes(
              node,
              new AVPair[] {
                    new AVPair( PredicateModel.IS_VISIBLE,
                                PredicateModelCore.getVisibleEnum().getElement(
                                  isVisible ) ),
                    new AVPair( PredicateModel.IS_STYLED,
                                isStyled ? Boolean.TRUE : Boolean.FALSE ) } );
            dumper.waitForBreak();
          } else if( cmd2 == "mi" ) { // move by setting index: "mi <old> <new>"
            final int loc1 = Integer.parseInt( st.nextToken() );
            final IRLocation irloc1 = predModel.location( loc1 );
            final int loc2 = Integer.parseInt( st.nextToken() );

            final IRNode node = predModel.elementAt( irloc1 );
            @SuppressWarnings("unchecked")
			final SlotInfo<Integer> si = predModel.getNodeAttribute( SequenceModel.INDEX );
            node.setSlotValue( si, Integer.valueOf(loc2) );
            dumper.waitForBreak();
          }
        }
      }
    }
  }
}

