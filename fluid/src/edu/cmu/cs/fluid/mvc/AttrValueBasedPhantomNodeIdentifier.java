/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttrValueBasedPhantomNodeIdentifier.java,v 1.9 2006/03/29 18:30:56 chance Exp $
 *
 * AttrValueBasedPhantomNodeIdentifier.java
 *
 * Created on January 24, 2002, 4:06 PM
 */

package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Phantom node identifier that identifies phantom nodes based on the value
 * of a particular model attribute.  The mapping is retrieved from a second
 * attribute.  (The same attribute may be used in both roles.)  The attributes
 * are given by their name because the at the time the identifier is created
 * the attribute storage will not (cannot) have been created yet.  (This seems
 * to be a frustrating side-effect of having to use delegation to simulate mixin
 * inheritance.)  This also means that the consturctor cannot verify that the
 * named attributes are actually part of the model, or that the map attribute
 * has the appropriate type.
 * 
 * @see PhantomNodeIdentifier.Factory
 * @see PhantomSupportingInheritedAttributeBuilder
 *
 * @author Aaron Greenhouse
 */
public class AttrValueBasedPhantomNodeIdentifier
extends AbstractAttrBasedPhantomNodeIdentifier
implements PhantomNodeIdentifier
{
  /**
   * The name of the attribute used for identifying phantom nodes.
   * This will be an interned String.  There are no restrictions on the
   * type of the attribute.
   */
  private final String idAttr;
  
  /**
   * The value that for the {@link #idAttr} that serves to identify a node
   * as being a phantom node.
   */
  private final Object identifyingValue;
  
  
  
  /**
   * Creates a new instance of AttrValueBasedPhantomNodeIdentifier.
   * @param model The model the identifier is associated with.
   * @param id The name of the attribute used for identifying phantom nodes.
   *   There are no restrictions on the type of the attribute.
   * @param map The name of the attribute used for mapping phantom nodes.
   *   The attribute should be of type {@link edu.cmu.cs.fluid.ir.IRNodeType}.  A
   *   class-cast exception will be thrown by {@link #mapPhantomNode} if this
   *   is not the case.
   * @param value The value that for the <code>id</code> attribute that
   * serves to identify a node as being a phantom node.
   */
  public AttrValueBasedPhantomNodeIdentifier(
    final Model model, final String id, final String map, final Object value )
  {
    super( model, map );
    idAttr = id.intern();
    identifyingValue = value;
  }

  /**
   * Query if a node is a phantom node.
   * @exception UnknownAttributeException Thrown if the id attribute is not
   * found in the model associated with this phantom node identifier.
   */
  @SuppressWarnings("unchecked")
  @Override
  public final boolean isPhantomNode( final IRNode node )
  {
    final SlotInfo attr = partOf.getNodeAttribute( idAttr );
    return identifyingValue.equals( node.getSlotValue( attr ) );
  }
}
