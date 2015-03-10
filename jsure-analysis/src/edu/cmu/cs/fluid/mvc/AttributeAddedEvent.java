/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeAddedEvent.java,v 1.9 2007/01/12 18:53:29 chance Exp $
 * AttributeAddedEvent.java
 *
 * Created on December 12, 2001, 9:09 AM
 */

package edu.cmu.cs.fluid.mvc;


/**
 * Event indicating a new attribute has been added to the sending model.
 *
 * @author Aaron Greenhouse
 */
public class AttributeAddedEvent extends ModelEvent
{
  /** Constant indicating the attribute is model-level. */
  public static final int MODEL_LEVEL = 1;
  
  /** Constant indicating the attributei s node-level. */
  public static final int NODE_LEVEL = 2;
  
  /** The name of the new attribute as an interned String. */
  private final String attr;
  
  /**
   * The type of the new attribute: either {@link #MODEL_LEVEL} or
   * {@link #NODE_LEVEL}.
   */
  private final int level;
  
  /**
   * Creates new AttributeAddedEvent.
   * @param src The model to which the attribute was added.
   * @param attr The name of the new attribute.
   * @param level The type of the new attribute: either {@link #MODEL_LEVEL} or
   *              {@link #NODE_LEVEL}.
   */
  public AttributeAddedEvent(
    final Model src, final String attr, final int level )
  {
    super( src );
    if( level != MODEL_LEVEL && level != NODE_LEVEL ) {
      throw new IllegalArgumentException( "Unknown value for level argument." );
    }
    this.attr = attr.intern();
    this.level = level;
  }

  /**
   * Get the name of the attribute.
   * @return The name of the newly added attribute as an interned String.
   */
  public String getAttributeName()
  {
    return attr;
  }
  
  /**
   * Test if the attribute is a model-level attribute.
   */
  public boolean isCompAttribute()
  {
    return (level == MODEL_LEVEL);
  }
  
  /**
   * Test if the attribute is a node-level attribute.
   */
  public boolean isNodeAttribute()
  {
    return (level == NODE_LEVEL);
  }
  
  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (AttributeAddedEvent): [" );
    buf.append( attr );
    buf.append( " isNodeAttr=" );
    buf.append( (level == NODE_LEVEL) );
    buf.append( ']' );
    return buf.toString();
  }
}
