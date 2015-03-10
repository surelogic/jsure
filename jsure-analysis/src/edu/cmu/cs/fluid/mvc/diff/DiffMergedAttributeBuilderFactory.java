/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/diff/DiffMergedAttributeBuilderFactory.java,v 1.7 2006/03/30 19:47:21 chance Exp $ */
package edu.cmu.cs.fluid.mvc.diff;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.attributes.*;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Fill this in...
 */
public final class DiffMergedAttributeBuilderFactory
implements BareAttributeMergingManager.MergedAttributeBuilderFactory
{
  /** The object used to id phantom nodes. */
  private final PhantomNodeIdentifier idPhantom;
  
  /**
   * The model-level attribute that controls switching between
   * the base and delta models for model-level attributes.
   */
  private final String compSwitcher;
  
  /**
   * The node-level attribute that controls switching between
   * the base and delta modesl for node-level attributes.
   */
  private final String nodeSwitcher;
  
  
  
  public DiffMergedAttributeBuilderFactory(
    final PhantomNodeIdentifier pni, final String comp,
    final String node )
  {
    idPhantom = pni;
    compSwitcher = comp;
    nodeSwitcher = node;
  }
  
  // inherit javadoc.
  @Override
  public BareAttributeMergingManager.MergedAttributeBuilder create()
  {
    return new DiffMergedAttributeBuilder( idPhantom, compSwitcher, nodeSwitcher );
  }
}


/**
 * Fill this in...
 */
final class DiffMergedAttributeBuilder
implements BareAttributeMergingManager.MergedAttributeBuilder
{
  /** The object used to id phantom nodes. */
  private final PhantomNodeIdentifier idPhantom;
  
  /**
   * The model-level attribute that controls switching between
   * the base and delta models for model-level attributes.
   */
  private final String compSwitcher;
  
  /**
   * The node-level attribute that controls switching between
   * the base and delta modesl for node-level attributes.
   */
  private final String nodeSwitcher;



  public DiffMergedAttributeBuilder(
    final PhantomNodeIdentifier pni, final String comp, final String node )
  {
    idPhantom = pni;
    compSwitcher = comp;
    nodeSwitcher = node;
  }


 
  /*
   * Rely on the fact that attributes are merged after the "regular"
   * attributes have been created.  This way we can do a look up
   * on the name of the switcher attribute and convert it to a 
   * CompontSlot.
   */
  @Override
  public <T> ComponentSlot<T> buildCompAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final ComponentSlot<T>[] attrs,
    final AttributeChangedCallback cb )
  {
    /*
     * Every attribute gets switched.
     */
    final ComponentSlot<T> switched =
      new AttrSwitchedDualModelAttribute<T>( 
            mutex, attr, attrs[0], attrs[1], 
            partOf.getCompAttribute( compSwitcher ), cb );
    
    /*
     * Determine the mutable status.
     */    
    ComponentSlot<T> wrapped = null;
    if( mode == AttributeMergingManager.MUTABLE_LOCAL_MERGED ) {
      wrapped = new MutableLocalInheritedModelAttribute<T>(
                      mutex, attr, switched, 
                      SimpleComponentSlotFactory.simplePrototype, cb );
    } else if( mode == AttributeMergingManager.IMMUTABLE_MERGED ) {
      wrapped = new GuardedImmutableModelAttribute<T>(
                      partOf, mutex, switched, attr );
    } else {
      throw new IllegalArgumentException( "Unknown inheritance mode." );
    }
    return wrapped;
  }
  
  /*
   * Rely on the fact that attributes are merged after the "regular"
   * attributes have been created.  This way we can do a look up
   * on the name of the switcher attribute and convert it to a 
   * CompontSlot.
   */
  @Override
  public <T> SlotInfo<T> buildNodeAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final SlotInfo<T>[] attrs,
    final AttributeChangedCallback cb )
  {
    /*
     * Every attribute is switched and phantom supporting.
     */
    final SlotInfo<T> switched = 
      new AttrSwitchedDualNodeAttribute<T>(
            partOf, mutex, attr, attrs[0], attrs[1], 
            partOf.getNodeAttribute( nodeSwitcher ), cb );
    final SlotInfo<T> phantomed = 
      new PhantomSupportingNodeAttribute<T>(
            partOf, mutex, switched, attr, idPhantom );

    /*
     * Determine the mutable status.
     */
    SlotInfo<T> wrapped = null;
    if( mode == AttributeMergingManager.MUTABLE_LOCAL_MERGED ) {
      wrapped = new MutableLocalInheritedNodeAttribute<T>(
                      partOf, mutex, attr, phantomed,
                      SimpleSlotFactory.prototype, cb );
    } else if( mode == AttributeMergingManager.IMMUTABLE_MERGED ) {
      wrapped = new GuardedImmutableNodeAttribute<T>( partOf, mutex, phantomed, attr );
    } else {
      throw new IllegalArgumentException( "Unknown inheritance mode." );
    }
    return wrapped;
  }
  
  /**
   * Considers the mode
   * {@link AttributeMergingManager#MUTABLE_LOCAL_MERGED}  to be mutable.
   */
  @Override
  public boolean isModeMutable( final Object mode )
  {
    return (mode == AttributeMergingManager.MUTABLE_LOCAL_MERGED);
  }
}
