/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractAttrBasedPhantomNodeIdentifier.java,v 1.9 2006/03/29 18:30:56 chance Exp $
 *
 * AbstractAttrBasedPhantomNodeIdentifier.java
 * Created on January 24, 2002, 4:06 PM
 */
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Abstract phantom node identifier that maps phantom nodes based on
 * a particular model.  The mapping is retrieved from a second
 * attribute.  The attribute
 * is given by name because at the time the identifier is created
 * the attribute storage will not (cannot) have been created yet.  (This seems
 * to be afrustrating side-effect of having to use delegation to simulate mixin
 * inheritance.)  This also means that the constructor cannot verify that the
 * named attribute is actually part of the model, or that it
 * has the appropriate type.
 * 
 * @see PhantomSupportingInheritedAttributeBuilderFactory
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractAttrBasedPhantomNodeIdentifier
implements PhantomNodeIdentifier
{
  /** The model the identifier is associated with. */
  protected final Model partOf;
    
  /**
   * The name of the attribute used for mapping phantom nodes.
   * This will be an interned String.  The attribute should be of type
   * {@link edu.cmu.cs.fluid.ir.IRNodeType}.  A class-cast exception will be thrown by 
   * {@link #mapPhantomNode} if this is not the case.
   */
  protected final String mapAttr;
  
  
  
  /**
   * Creates a new instance of AttrValueBasedPhantomNodeIdentifier.
   * @param model The model the identifier is associated with.
   * @param map The name of the attribute used for mapping phantom nodes.
   *   The attribute should be of type {@link edu.cmu.cs.fluid.ir.IRNodeType}.
   *   A class-cast exception will be thrown by {@link #mapPhantomNode} if
   *   this is not the case.
   */
  public AbstractAttrBasedPhantomNodeIdentifier(
    final Model model, final String map )
  {
    partOf = model;
    mapAttr = map.intern();
  }

  /**
   * Query if a node is a phantom node.
   */
  @Override
  public abstract boolean isPhantomNode( IRNode node );
   
  /**
   * Map a phantom node to the node that represents it's current position
   * within the model structure.
   * @exception UnknownAttributeException Thrown if the map attribute is not
   * found in the model associated with this phantom node identifier.
   */
  @Override
  @SuppressWarnings("unchecked")
  public final IRNode mapPhantomNode( final IRNode node )
  {
    final SlotInfo<IRNode> attr = partOf.getNodeAttribute( mapAttr );
    return node.getSlotValue( attr );
  }
}
