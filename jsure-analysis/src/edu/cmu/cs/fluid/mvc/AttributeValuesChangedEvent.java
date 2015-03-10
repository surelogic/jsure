/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeValuesChangedEvent.java,v 1.10 2007/01/12 18:53:29 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;

import com.surelogic.common.util.*;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * 
 */
public class AttributeValuesChangedEvent
extends ModelEvent
{
  //============================================================
  //== Inner class
  //============================================================

  /**
   * Record of the attributes that changed, and their new values, for
   * a single node.  The node is <code>null</code> if the attributes
   * are component-level attributes.
   */
  public static class ChangeRecord 
  {
    /**
     * A node whose attributes changed, or <code>null</code> if the
     * attributes in {@link #pairs} are component-level.
     */
    private final IRNode node;

    /**
     * The attributes whose value changed, and their new values,
     * as attribute&ndash;value pairs.
     */
    private final AVPair[] pairs;

    public ChangeRecord( final IRNode n, final AVPair[] p )
    {
      node = n;
      pairs = p;
    }

    /**
     * Get the node whose attributes changed, or <code>null</code> if
     * the attributes are component-level.
     */
    public IRNode getNode() { return node; }

    /**
     * Get an iterator over the attributes that changed, and their
     * new values.
     * @return An iterator over {@link edu.cmu.cs.fluid.mvc.AVPair}s.
     */
    public Iterator<AVPair> getAVPairs() { return new ArrayIterator<AVPair>( pairs ); }
    
    @Override
    public String toString()
    {
      final StringBuilder buf = new StringBuilder( "[" );
      if( node != null ) {
        buf.append( node.toString() );
        buf.append( ' ' );
      }
      int i = 0;
      for( ; i < pairs.length-1; i++ ) {
        buf.append( pairs[i] );
        buf.append( ' ' );
      }
      buf.append( pairs[i] );
      buf.append( ']' );
      return buf.toString();
    }
  }


  //============================================================
  //== Fields
  //============================================================

  /**
   * Array of {@link ChangeRecord}s, one for each node whose
   * attribute value changed.  <code>null</code> if no node-level
   * attributes changed.
   */
  private final ChangeRecord[] changedNodes;

  /**
   * {@link ChangeRecord} for component-level attributes.
   * <code>null</code> if no component-level attributs changed.
   */
  private final ChangeRecord changedComponent;



  //============================================================
  //== Constructors
  //============================================================

  /**
   * Indicate that several component-level attributes changed.
   * @param model The model that changed.
   * @param pairs Attribute&ndash;value pairs of the component-level
   * attributes that changed and their new values.
   */
  public AttributeValuesChangedEvent( final Model model, final AVPair[] pairs )
  {
    super( model );
    changedNodes = null;
    changedComponent = new ChangeRecord( null, pairs );
  }

  /**
   * Indicate that a signle component-level attribute changed.
   * @param model The model that changed.
   * @param attr The model-level attribute that changed; interned String.
   * @param value The new value of the attribute.
   */
  public AttributeValuesChangedEvent( final Model model, final String attr,
                                      final Object value )
  {
    this( model, new AVPair[] { new AVPair( attr, value ) } );
  }

  /**
   * Indicate that attribute values for a single node have changed.
   * @param model The model that changed.
   * @param node The node whose attribute values changed.
   * @param pairs Attribute&ndash;value pairs of the node-level
   * attributes that changed and their new values.
   */
  public AttributeValuesChangedEvent( final Model model, final IRNode node,
				      final AVPair[] pairs )
  {
    super( model );
    changedNodes = new ChangeRecord[] { new ChangeRecord( node, pairs ) };
    changedComponent = null;
  }

  /**
   * Indicate that a single attribute value for a single node has changed.
   * @param model The model that changed.
   * @param node The node whose attribute value changed.
   * @param attr The attribute whose value changed; interned String;
   * @param value The new attribute value.
   */
  public AttributeValuesChangedEvent( final Model model, final IRNode node,
				      final String attr, final Object value )
  {
    this( model, node, new AVPair[] { new AVPair( attr, value ) } );
  }
  


  //============================================================
  //== Methods
  //============================================================

  /**
   * Get an iterator over the {@link ChangeRecord}s for the 
   * nodes whose attribute values changed.
   */
  public Iterator<ChangeRecord> getChangedNodes()
  {
    if( changedNodes == null ) {
      return new EmptyIterator<ChangeRecord>();
    } else {
      return new ArrayIterator<ChangeRecord>( changedNodes );
    }
  }

  /**
   * Get an iterator over {@link AVPair}s representing the changed
   * component-level attributes, and their new values.
   */
  public Iterator<AVPair> getChangedAttributes()
  {
    if( changedComponent == null ) {
      return new EmptyIterator<AVPair>();
    } else {
      return changedComponent.getAVPairs();
    }
  }
  
  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (AttributeValuesChangedEvent): [" );
    if( changedComponent != null ) buf.append( changedComponent.toString() );
    if( changedNodes != null ) {
      int i = 0;
      for( ; i < changedNodes.length-1; i++ ) {
        buf.append( changedNodes[i].toString() );
        buf.append( ' ' );
      }
      buf.append( changedNodes[i].toString() );
    }
    buf.append( ']' );
    return buf.toString();
  }
}
