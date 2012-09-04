/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/TestSortedAttributeView.java,v 1.8 2005/07/01 16:15:36 chance Exp $
 *
 * TestSortedAttributeView.java
 * Created on March 12, 2002, 2:06 PM
 */

package edu.cmu.cs.fluid.mvc.attr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.sequence.LabeledSequence;
import edu.cmu.cs.fluid.mvc.sequence.SequenceDumper;
import edu.cmu.cs.fluid.mvc.sequence.SimpleLabeledSequenceFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;

/**
 * Program to test SortedAttributeView implementations.
 * 
 * @author Aaron Greenhouse
 */
public class TestSortedAttributeView
{
  public static void main( final String[] args )
  throws Exception
  {
    // Init source sequence
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence", SimpleSlotFactory.prototype );

    // Init AttributeModel
    final AttributeModel attrModel = 
      SimpleAttributeViewFactory.prototype.create( "Attribute Model", seq );

    // init list of modeled attributes
    final IRNode[] modeledAttrs = new IRNode[attrModel.size()];
    final Iterator<IRNode> nodes = attrModel.getNodes();
    for( int count = 0; nodes.hasNext(); ) {
      modeledAttrs[count++] = nodes.next();
    }

    // Init the list of valid sort attributes
    final List<String> sortAttrs = new ArrayList<String>( 10 );
    final Iterator<String> iter = attrModel.getNodeAttributes();
    while( iter.hasNext() ) sortAttrs.add( iter.next() );

    final SortedAttributeView sortedAttrView = 
      SortedAttributeViewFactory.mutSrcPrototype.create(
        "Sorted View", attrModel, AttributeModel.ATTR_NAME, true,
        AttributeInheritancePolicy.nullPolicy );

    final SequenceDumper dumper =
      new SequenceDumper( sortedAttrView, System.out );

    final BufferedReader input =
      new BufferedReader( new InputStreamReader( System.in ) );
    PickledAttributeModelState pickledState = null;
    
    while( true ) {
      System.out.println( "save, restore, help, quit, label # <label>, ascend, descend, sort #" );
      System.out.print( "> " );
      System.out.flush();

      final StringTokenizer st = new StringTokenizer( input.readLine() );
      final String cmd = st.nextToken().intern();

      if( cmd == "save" ) { // save state 
        pickledState = sortedAttrView.getPickledState();
        System.out.println( "\nPickle = " + pickledState );
      } else if( cmd == "restore" ) { // restore from pickle
        sortedAttrView.setStateFromPickle( pickledState );
        dumper.waitForBreak();
      } else if( cmd == "help" ) {
        System.out.println( "Attributes being modeled" );
        for( int i = 0; i < modeledAttrs.length; i++ ) {
          System.out.println(
            i + " = " + attrModel.getName( modeledAttrs[i] ) );
        }
        System.out.println();
        System.out.println( "Valid sort attributes (attrs of the attribute model)" );
	for( int i = 0; i < sortAttrs.size(); i++ ) {
	  System.out.println( i + " = " + sortAttrs.get( i ) );
	}
	System.out.println();
      } else if( cmd == "quit" ) {
	System.exit( 0 );
      } else if( cmd == "label" ) {
        final IRNode n = modeledAttrs[Integer.parseInt( st.nextToken() )];
        final String l = st.nextToken();
        sortedAttrView.setLabel( n, l );
        dumper.waitForBreak();
      } else if( cmd == "ascend" ) {        
        sortedAttrView.setAscending( true );
        dumper.waitForBreak();
      } else if( cmd == "descend" ) {
        sortedAttrView.setAscending( false );
        dumper.waitForBreak();
      } else if( cmd == "sort" ) {
        final int attrIdx = Integer.parseInt( st.nextToken() );
        final String attr = sortAttrs.get( attrIdx );
        try {
          sortedAttrView.setSortAttribute( attr );
          dumper.waitForBreak();
        } catch( IllegalArgumentException e ) {
          System.out.println( "Error: " + e.getMessage() );
        }
      }        
    }
  }
}
