/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/NewAttributesEvent.java,v 1.8 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;


/**
 * Event sent by a model that indicates that new attributes 
 * (both node- and component-level) have been added to the
 * model.
 *
 * @see ModelListener
 * @author Aaron Greenhouse
 */
public class NewAttributesEvent
extends ModelEvent
{
  //============================================================
  //== Fields
  //============================================================

  /**
   * Array of {@link java.lang.String#intern}ed Strings, giving
   * the names of the new component-level attributes.
   */
  private final String[] compAttrs;

  /**
   * Array of {@link java.lang.String#intern}ed Strings, giving
   * the names of the new node-level attributes.
   */
  private final String[] nodeAttrs;



  //============================================================
  //== Constructor
  //============================================================


  /**
   * Creates a new <code>NewAttributesEvent</code> instance.
   *
   * @param source The model in which the new attributes were defined.
   * @param comps Array of {@link java.lang.String#intern}ed Strings, giving
   * the names of the new component-level attributes.
   * @param nodes Array of {@link java.lang.String#intern}ed Strings, giving
   * the names of the new node-level attributes.
   */
  public NewAttributesEvent(
    final Model source, final String[] comps, final String[] nodes )
  {
    super( source );
    compAttrs = comps;
    nodeAttrs = nodes;
  }

  /**
   * Get an array of {@link java.lang.String#intern}ed Strings naming
   * the newly added component-level eattributes.
   */
  public String[] getComponentAttributes()
  {
    final String[] copy = new String[compAttrs.length];
    System.arraycopy( compAttrs, 0, copy, 0, copy.length );
    return copy;
  }

  /**
   * Get an array of {@link java.lang.String#intern}ed Strings naming
   * the newly added node-level eattributes.
   */
  public String[] getNodeAttributes()
  {
    final String[] copy = new String[nodeAttrs.length];
    System.arraycopy( nodeAttrs, 0, copy, 0, copy.length );
    return copy;
  }
  
  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (NewAttributesEvent): [Model[" );
    int i = 0;
    for( ; i < compAttrs.length-1; i++ ) {
      buf.append( compAttrs[i] );
      buf.append( ' ' );
    }
    buf.append( compAttrs[i] );
    
    buf.append( "] Node[" );
    i = 0;
    for( ; i < nodeAttrs.length-1; i++ ) {
      buf.append( nodeAttrs[i] );
      buf.append( ' ' );
    }
    buf.append( nodeAttrs[i] );
   
    buf.append( "]]" );
    return buf.toString();
  }
}

