/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/ConfigurableSequenceViewImpl.java,v 1.14 2007/07/10 22:16:30 aarong Exp $
 *
 * ConfigurableSequenceViewImpl.java
 * Created on March 28, 2002, 4:34 PM
 */
package edu.cmu.cs.fluid.mvc.sequence;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.*;

/**
 * A view of an <em>unversioned</em> sequence that allows for 
 * nodes to be ellided.  The exported model can contain nodes
 * for which {@link #isEllipsis} is <code>true</code>.
 *
 * @author Aaron Greenhouse
 */
final class ConfigurableSequenceViewImpl
extends AbstractSequenceToSequenceStatefulView
implements ConfigurableSequenceView
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
  
  /** Attribute storate for the ellipsis policy attribute */
  private final ComponentSlot<SequenceEllipsisPolicy> ellipsisAttr;

  /** Set of nodes that have proxy nodes. */
  private final Set<IRNode> proxyWearing;


  
  //===========================================================
  //== Constructor
  //===========================================================
  
  protected ConfigurableSequenceViewImpl(
    final String name, final SequenceModel src, final VisibilityModel vizModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final SequenceModelCore.Factory smf, final ConfigurableViewCore.Factory cvf,
    final AttributeInheritancePolicy aip, final ProxyAttributePolicy ap )
  throws SlotAlreadyRegisteredException
  {
    // Init model parts
    super( name, src, mf, vf, smf,
           LocalAttributeManagerFactory.prototype, 
           ProxySupportingAttributeManagerFactory.prototype );
    srcVizModel = vizModel;

    proxyWearing = new HashSet<IRNode>();
    configViewCore = 
      cvf.create( name, this, structLock, src, attrManager, inheritManager,
                  new IsHiddenChangedCallback() );
    attrPolicy = aip;
    proxyPolicy = ap;

    // Init model attributes
    ellipsisAttr =
      new SimpleComponentSlot<SequenceEllipsisPolicy>(
            SequenceEllipsisPolicyType.prototype, SimpleExplicitSlotFactory.prototype,
	    NoEllipsisSequenceEllipsisPolicy.prototype );
    attrManager.addCompAttribute(
      ELLIPSIS_POLICY, Model.STRUCTURAL, ellipsisAttr,
      new SequenceViewChangedCallback() );
    configViewCore.setSourceModels( src, viewCore, srcVizModel );
    
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
    srcVizModel.addModelListener( srcModelBreakageHandler );
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

  private class SequenceViewChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( attr == ELLIPSIS_POLICY ) {
        signalBreak( 
          new AttributeValuesChangedEvent(
                ConfigurableSequenceViewImpl. this, attr, val ) );
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
        signalBreak(
          new AttributeValuesChangedEvent(
                ConfigurableSequenceViewImpl.this, node, attr, val ) );
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
  public void setSequenceEllipsisPolicy( final SequenceEllipsisPolicy p )
  {
    synchronized( structLock ) {
      ellipsisAttr.setValue( p );
    }
    signalBreak( new AttributeValuesChangedEvent( this, ELLIPSIS_POLICY, p ) );
  }

  /**
   * Get the ellipsis policy
   */
  @Override
  public SequenceEllipsisPolicy getSequenceEllipsisPolicy()
  {
    synchronized( structLock ) {
      return ellipsisAttr.getValue();
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
  protected void rebuildModel( final List events )
  throws InterruptedException
  {
    synchronized( structLock ) {
      // Clear the existing sequence model...
      // And reset the proxy flags
      seqModCore.clearModel(); 
      final Iterator proxyIter = proxyWearing.iterator();
      while( proxyIter.hasNext() ) {
	final IRNode n = (IRNode) proxyIter.next();
	configViewCore.removeProxyFrom( n );
	proxyIter.remove();
      }

      // Reset the ellipsis policy
      final SequenceEllipsisPolicy ellipsisPolicy =
	getSequenceEllipsisPolicy();
      if( ellipsisPolicy != null ) ellipsisPolicy.resetPolicy();

      // Build the new model
      int currentLoc = 0;
      final Iterator nodes = srcModel.getNodes();
      while( nodes.hasNext() ) {
        if( Thread.interrupted() ) throw new InterruptedException();
        final IRNode node = (IRNode) nodes.next();
        final boolean hidden = configViewCore.isHidden( node );
        final boolean vis = srcVizModel.isVisible( node );
        final boolean showNode = !hidden && vis;
        
        if( showNode ) {
          seqModCore.appendElement( node );
          currentLoc += 1;
        } else {
          if( ellipsisPolicy != null ) {
            if( vis ) ellipsisPolicy.nodeSkipped( node, currentLoc );
          }
        }
      }
      
      // Insert ellipses
      if( Thread.interrupted() ) throw new InterruptedException();
      if( ellipsisPolicy != null ) ellipsisPolicy.applyPolicy();
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
  //-- Begin ModelToSequenceStatefulView Portion 
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Ellipsis Insertion
  //===========================================================

  /**
   * Called by the {@link SequenceEllipsisPolicy} to insert
   * an ellipsis.
   * @param pos The index before which the ellipsis should be placed.
   * @param nodes The set of nodes that the ellipsis is replacing.
   */
  // Called by ellipsis policy, which is called by the Rebuilder,
  // running in the monitor.
  @Override
  public void insertEllipsisBefore( final IRLocation pos, final Set<IRNode> nodes )
  {
    final IRNode node = new PlainIRNode();
    seqModCore.insertElementBefore( node, pos );
    initEllipsis( node, nodes );
  }

  /**
   * Called by the {@link SequenceEllipsisPolicy} to insert
   * an ellipsis.
   * @param pos The index after which the ellipsis should be placed.
   * @param nodes The set of nodes that the ellipsis is replacing.
   */
  // Called by ellipsis policy, which is called by the Rebuilder,
  // running in the monitor.
  @Override
  public void insertEllipsisAfter( final IRLocation pos, final Set<IRNode> nodes )
  {
    final IRNode node = new PlainIRNode();
    seqModCore.insertElementAfter( node, pos );
    initEllipsis( node, nodes );
  }

  private void initEllipsis( final IRNode ellipsis, final Set<IRNode> nodes )
  {
    modelCore.setEllipsis( ellipsis, true );
    modelCore.setEllidedNodes( ellipsis, nodes );
    proxyWearing.add( ellipsis );
    configViewCore.setProxyNodeAttributes(
      configViewCore.generateProxyFor( ellipsis ),
      proxyPolicy.attributesFor( this, nodes ) );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End ModelToSequenceStatefulView Portion 
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
