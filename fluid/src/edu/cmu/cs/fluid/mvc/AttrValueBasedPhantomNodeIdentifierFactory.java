/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttrValueBasedPhantomNodeIdentifierFactory.java,v 1.7 2003/07/15 21:47:18 aarong Exp $
 *
 * AttrValueBasedPhantomNodeIdentifierFactory.java
 * Created on January 24, 2002, 5:01 PM
 */

package edu.cmu.cs.fluid.mvc;


/**
 * Factory for creating {@link AttrValueBasedPhantomNodeIdentifier} instances.
 *
 * @author Aaron Greenhouse
 */
public final class AttrValueBasedPhantomNodeIdentifierFactory
implements PhantomNodeIdentifier.Factory
{
  /**
   * The name of the attribute used for identifying phantom nodes.
   */
  private final String idAttr;

  /**
   * The name of the attribute used for mapping phantom nodes.

   */
  private final String mapAttr;

  /**
   * The value that for the {@link #idAttr} that serves to identify a node
   * as being a phantom node.
   */
  private final Object identifyingValue;
  
  
  
  /**
   * Creates a new instance of AttrValueBasedPhantomNodeIdentifierFactory.
   * @param id The name of the attribute used for identifying phantom nodes.
   * There are no restrictions on the type of the attribute.
   * @param map The name of the attribute used for mapping phantom nodes.
   * The attribute should be of type {@link edu.cmu.cs.fluid.ir.IRNodeType}.  A class-cast
   * exception will be thrown by {@link #mapPhantomNode} method of the returned 
   * identifier if this is not the case.
   * @param val The value that for the {@link #idAttr} that serves to identify
   a node as being a phantom node.
   */
  public AttrValueBasedPhantomNodeIdentifierFactory(
    final String id, final String map, final Object val )
  {
    idAttr = id;
    mapAttr = map;
    identifyingValue = val;
  }

  /**
   * Create an new attribute-value&ndash;based phantom node identifier object.
   * @param model The model the identifier is to be associated with.
   */
  @Override
  public PhantomNodeIdentifier create( final Model model )
  {
    return new AttrValueBasedPhantomNodeIdentifier(
                 model, idAttr, mapAttr, identifyingValue );
  }
}
