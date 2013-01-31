/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/ConfigurableSetViewImpl.java,v 1.12 2007/07/10 22:16:37 aarong Exp $ */

package edu.cmu.cs.fluid.mvc.set;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.*;

/**
 * Minimal implementation of {@link ConfigurableSetView}.
 *
 * @author Aaron Greenhouse
 */
final class ConfigurableSetViewImpl
extends AbstractSetToSetStatefulView
implements ConfigurableSetView
{
  //===========================================================
  //== Fields
  //===========================================================

  /** The ConfigurableViewCore delegate object. */
  protected final ConfigurableViewCore configViewCore;

  /** The source visibility model. */
  protected final VisibilityModel srcVizModel;

  /** The Attribute Inheritance Policy */
  protected final AttributeInheritancePolicy attrPolicy;

  /** The Attribute Policy */
  protected final ProxyAttributePolicy proxyPolicy;

  /** Storage for the {@link ConfigurableSetView#ELLIPSIS_POLICY} attribute. */
  private final ComponentSlot<Boolean> ellipsisAttr;

  /** The node used to represent an ellipsis. */
  protected final IRNode ellipsisNode;


  
  //===========================================================
  //== Constructor
  //===========================================================

  protected ConfigurableSetViewImpl(
    final String name, final SetModel src, final VisibilityModel vizModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final SetModelCore.Factory setmf, final ConfigurableViewCore.Factory cvf,
    final AttributeInheritancePolicy aip, final ProxyAttributePolicy ap,
    final Boolean ellipsisPolicy )
  throws SlotAlreadyRegisteredException
  {
    // Init model parts
    super( name, src, mf, vf, setmf, LocalAttributeManagerFactory.prototype, 
           ProxySupportingAttributeManagerFactory.prototype );
    ellipsisNode = new PlainIRNode();

    srcVizModel = vizModel;
    configViewCore =
      cvf.create( name, this, structLock, src, attrManager, inheritManager,
                  new IsHiddenChangedCallback() );
    attrPolicy = aip;
    proxyPolicy = ap;

    // Init model attributes
    ellipsisAttr = 
      new SimpleComponentSlot<Boolean>(
            IRBooleanType.prototype, SimpleExplicitSlotFactory.prototype,
            ellipsisPolicy );
    attrManager.addCompAttribute(
      ELLIPSIS_POLICY, Model.STRUCTURAL, ellipsisAttr, 
      new SetViewChangedCallback() );
    
    configViewCore.setSourceModels( src, viewCore, vizModel );
    
    // inherit attributes
    inheritManager.inheritAttributesFromModel(
      src, attrPolicy, AttributeChangedCallback.nullCallback );
    
    // Initialize the model contents
    rebuildModel();

    // Connect model-view chain
    // Do not add listener to the regular srcModel because 
    // the visibility model also breaks when the srcModel does
    // so if we just listen to the visibility model things will 
    // work.  If we listen to both we get double rebuilds which are
    // annoying.  CAVEAT: VisibilityModel must always listen to
    // the source model.
    // srcModel.addModelListener( srcModelBreakageHandler );
    vizModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }
  


  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Local State  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Callbacks
  //===========================================================

  private class SetViewChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( attr == ELLIPSIS_POLICY ) {
        signalBreak( new AttributeValuesChangedEvent(
                           ConfigurableSetViewImpl. this, attr, val ) );
      }
    }
  }

  private class IsHiddenChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( attr == ConfigurableView.IS_HIDDEN ) {
        signalBreak( new AttributeValuesChangedEvent(
                           ConfigurableSetViewImpl. this, node, attr, val ) );
      }
    }
  }



  //===========================================================
  //== Ellipsis Control
  //===========================================================

  /**
   * Set the ellipsis policy.
   */
  @Override
  public void setSetEllipsisPolicy( final boolean p )
  {
    synchronized( structLock ) {
      ellipsisAttr.setValue( (p ? Boolean.TRUE : Boolean.FALSE) );
    }
    signalBreak( new ModelEvent( this ) );
  }

  /**
   * Get the ellipsis policy
   */
  @Override
  public boolean getSetEllipsisPolicy()
  {
    synchronized( structLock ) {
      final Boolean b = ellipsisAttr.getValue();
      return b.booleanValue();
    }
  }



  //===========================================================
  //== Rebuild methods
  //===========================================================

  @Override
  protected void attributeAddedToSource( 
    final Model src, final String attr, final boolean isNodeLevel )
  {
    /*
     * XXX Should really try to inherit the new attribute here.
     * XXX The event handling/rebuild stuff needs
     * XXX to be globally redone for this to be dealt with properly.  It can
     * XXX be done with the current system, but it is not easy to do.
     */
  }

  
  
  /**
   * This causes the source model to be traversed and the
   * sub-model to be built.
   */
  @Override
  protected void rebuildModel( final List events ) throws InterruptedException
  {
    synchronized( structLock ) {
      // Clear the existing sequence model...
      setModCore.clearModel();

      // Reset the ellipsis policy
      final boolean ellipsisPolicy =
        (ellipsisAttr.getValue()).booleanValue();
      boolean needEllipsis = false;
      final Set<IRNode> skipped = new HashSet<IRNode>();

      // Build the new model
      final Iterator nodes = srcModel.getNodes();
      while( nodes.hasNext() ) {
        if( Thread.interrupted() ) throw new InterruptedException();
        
        final IRNode node = (IRNode) nodes.next();
        final boolean hidden = configViewCore.isHidden( node );
        final boolean vis = srcVizModel.isVisible( node );
        final boolean showNode = !hidden && vis;
        
        if( showNode ) {
          setModCore.addNode( node );
        } else {
          needEllipsis = true;
          skipped.add( node );
        }
      }

      if( Thread.interrupted() ) throw new InterruptedException();
      if( ellipsisPolicy && needEllipsis ) {
        setModCore.addNode( ellipsisNode );
        modelCore.setEllipsis( ellipsisNode, true );
        modelCore.setEllidedNodes( ellipsisNode, skipped );
	configViewCore.setProxyNodeAttributes(
          configViewCore.generateProxyFor( ellipsisNode ),
          proxyPolicy.attributesFor( this, skipped ) );
      } else {
	configViewCore.removeProxyFrom( ellipsisNode );
      }
    }

    // Break our views
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Local State  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin ConfigurableView Portion 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  @Override
  public boolean isProxyNode( final IRNode node )
  {
    synchronized( structLock ) {
      return configViewCore.isProxyNode( node );
    }
  }

  @Override
  public IRNode getProxyNode( final IRNode node )
  {
    synchronized( structLock ) {
      return configViewCore.getProxyNode( node );
    }
  }

  @Override
  public boolean isHidden( final IRNode node )
  {
    synchronized( structLock ) {
      return configViewCore.isHidden( node );
    }
  }

  @Override
  public boolean isShown( final IRNode node )
  {
    synchronized( structLock ) {
      return configViewCore.isShown( node );
    }
  }
 
  @Override
  public void setHidden( final IRNode node, final boolean isHidden )
  {
    synchronized( structLock ) {
      configViewCore.setHidden( node, isHidden );
    }
    signalBreak( new AttributeValuesChangedEvent(
                       this, node, ConfigurableView.IS_HIDDEN,
                       isHidden ? Boolean.TRUE : Boolean.FALSE ) );
  }

  @Override
  public void setHiddenForAllNodes( final boolean isHidden )
  {
    synchronized( structLock ) {
      configViewCore.setHiddenForAllNodes( srcModel, isHidden );
    }
    signalBreak( new ModelEvent( this ) );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End ConfigurableView Portion 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
   * Override implementation to return <code>true</code> if the node 
   * is a proxy node.
   */
  @Override
  public boolean isOtherwiseAttributable( final IRNode node )
  {
    synchronized( structLock ) {
      return configViewCore.isProxyNode( node );
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}


