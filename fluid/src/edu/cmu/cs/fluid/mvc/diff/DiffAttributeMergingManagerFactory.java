/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/diff/DiffAttributeMergingManagerFactory.java,v 1.6 2003/07/15 18:39:13 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.diff;

import edu.cmu.cs.fluid.mvc.*;

/**
 * Fill this in...
 */
public final class DiffAttributeMergingManagerFactory
implements AttributeMergingManager.Factory
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
   * The value that for {@link #idAttr} that serves to identify a node
   * as being a phantom node.
   */
  private final Object identifyingValue;

  /**
   * The attribute used to control switching of model-level attributes.
   */
  private final String compSwitch;
 
  /**
   * The attribute used to control switching of node-level attributes.
   */
  private final String nodeSwitch;

  
  /**
   * Construct a new AttributeMergingManager Factory object.
   * @param id The name of the attribute used for identifying phantom nodes.
   * @param map The name of the attribute used for mapping phantom nodes.
   * @param idVal The value for the id attribute that serves to identify a node
   *   as being a phantom node.
   * @param comp The attribute used to control switching of model-level attributes.
   * @param node The attribute used to control switching of node-level attributes.
   */
  public DiffAttributeMergingManagerFactory(
    final String id, final String map, final Object idVal,
    final String comp, final String node )
  {
    idAttr = id;
    mapAttr = map;
    identifyingValue = idVal;
    compSwitch = comp;
    nodeSwitch = node;
  }
  
  /**
   * Create a new attribute merging manager for a particular model.
   * @param model The model whose attribute merging is to be managed
   *             by the new instance.
   * @param mutex The lock used to protect the state of the model.
   * @param attrManager The attribute manager of the model.
   */
  @Override
  public AttributeMergingManager create(
    final Model model, final Object mutex, final AttributeManager attrManager )
  {
    return new BareAttributeMergingManager(
                 model, mutex, attrManager,
                 new DiffMergedAttributeBuilderFactory( 
                       new AttrValueBasedPhantomNodeIdentifier(
                             model, idAttr, mapAttr, identifyingValue ),
                       compSwitch, nodeSwitch ) );
  }
}
