package edu.cmu.cs.fluid.mvc.visibility;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractModelToVisibilityStatefulView
extends AbstractModelToModelStatefulView
{
  /** The VisibilityModelCore delegate */
  protected final VisibilityModelCore visModCore;

  /** Local reference to the model whose visibility is being modeled */
  protected final Model srcModel;
  


  //===========================================================
  //== Constructor
  //===========================================================

  // Subclass must init SRC_MODELS attribute!
  public AbstractModelToVisibilityStatefulView(
    final String name, final ModelCore.Factory mf, final ViewCore.Factory vf,
    final VisibilityModelCore.Factory vmf, final Model src )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, mf.getFactory(),
           LocalAttributeManagerFactory.prototype,
           NullAttributeInheritanceManagerFactory.prototype );
    srcModel = src;
    visModCore = vmf.create( name, this, structLock, src, attrManager );
  }


  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  // inherit javadoc
  @Override
  public final void addNode( final IRNode n, final AVPair[] vals )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  // inherit javadoc
  @Override
  public final void removeNode( final IRNode n )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  // inherit javadoc
  @Override
  public final Iterator<IRNode> getNodes()
  {
    synchronized( structLock ) {
      return srcModel.getNodes();
    }
  }
  
  // inherit javadoc
  @Override
  public final boolean isPresent( final IRNode node )
  {
    /*
     * We always contain the same nodes as our source model, so just
     * delegate to the src model's isPresent.
     */
    return srcModel.isPresent( node );
  }
  
  /**
   * Delegates to the same method of the source model.
   */
  @Override
  public String idNode( final IRNode node )
  {
    return srcModel.idNode( node );
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin AbstractModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
   * Invoked when a new attribute is added to a source model.  Visibility
   * views don't do anything with the attributes of source models, so we
   * implement the method here to be a noop.
   */
  @Override
  protected final void attributeAddedToSource(
    final Model src, final String attr, final boolean isNodeLevel )
  {
    /*
     * Don't care about the attributes of source models.
     */
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End AbstractModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin VisibilityModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  public final boolean isVisible( final IRNode node )
  {
    synchronized( structLock ) {
      return visModCore.isVisible( node );
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End VisibilityModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
