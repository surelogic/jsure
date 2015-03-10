/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/AbstractPredicateBasedVisibilityView.java,v 1.9 2006/03/30 16:20:26 chance Exp $
 *
 * AbstractPredicateBasedVisibilityView.java
 * Created on May 21, 2002, 10:13 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.predicate.AttributePredicate;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;

/**
 * A minimal implemenation of {@link PredicateBasedVisibilityView}.
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractPredicateBasedVisibilityView
extends AbstractModelToVisibilityStatefulView
implements PredicateBasedVisibilityView
{
  //===========================================================
  //== Fields
  //===========================================================
  
  /** Reference to the predicate model used to control node  visibility. */
  private final PredicateModel srcPredModel;
  
  /**
   * Reference to the attribute value storage for the
   * {@link PredicateBasedVisibilityView#DEFAULT_VISIBILITY} attribute.
   */
  private final ComponentSlot<Boolean> defaultViz;
  
  /**
   * A local cache of the predicate information is stored here.  This 
   * is to avoid repeatedly querying the attributes of the PrecateModel,
   * which is expected to change much less frequently than the source model.
   * A list of {@link PredicateBasedVisibilityView.VizInfo} objects.
   */
  private final List<VizInfo> vizInfo = new ArrayList<VizInfo>();
  
  
  
  //===========================================================
  //== Constructor
  //===========================================================

  protected AbstractPredicateBasedVisibilityView(
    final String name, final Model srcModel, final PredicateModel predModel,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final VisibilityModelCore.Factory vmf )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, vmf, srcModel );
    srcPredModel = predModel;

    /* Initialize Model-level attributes */
    final IRSequence<Model> srcModels =
      ConstantSlotFactory.prototype.newSequence(2);
    srcModels.setElementAt( srcModel, 0 );
    srcModels.setElementAt( predModel, 1 );
    viewCore.setSourceModels( srcModels );

    defaultViz = SimpleComponentSlotFactory.simplePrototype.predefinedSlot( 
                   IRBooleanType.prototype, Boolean.TRUE );
    attrManager.addCompAttribute(
      DEFAULT_VISIBILITY, Model.INFORMATIONAL, true, defaultViz, 
      new DefaultVizChangedCallback() );

    /* init node-level attributes */
    final SlotInfo<Boolean> isVisible = 
      SimpleSlotFactory.prototype.newAttribute(
        name + "-" + VisibilityModel.IS_VISIBLE, IRBooleanType.prototype );
    attrManager.addNodeAttribute(
      VisibilityModel.IS_VISIBLE, Model.STRUCTURAL, isVisible );
    visModCore.setIsVisibleAttribute( isVisible );
  }

  /**
   * Calls the inherited implementation, and then checks that the 
   * predicate model is a view of the source model.
   */
  @Override
  public void finalizeInitialization()
  {
    super.finalizeInitialization();
    if( srcPredModel.getCompAttribute(
          PredicateModel.PREDICATES_OF ).getValue() != srcModel ) {
      throw new RuntimeException(   "PredicateModel \""
                                  + srcPredModel.getName()
                                  + "\" is not a view of Model \""
                                  + srcModel.getName() + "\"" );
    }
  }

  
  
  //===========================================================
  //== Callbacks
  //===========================================================

  /**
   * Attribute changed callback for catching changes to the 
   * {@link PredicateBasedVisibilityView#DEFAULT_VISIBILITY} attribute.
   * Caused the model to rebuild.
   */
  private class DefaultVizChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      if( attr == DEFAULT_VISIBILITY ) {
         signalBreak( new AttributeValuesChangedEvent(
                            AbstractPredicateBasedVisibilityView. this,
                            DEFAULT_VISIBILITY, val ) );
      }
    }
  }

  
  
  //===========================================================
  //== Other affiliated classes used internally
  //===========================================================

  /**
   * Immutable "Record" class used to cache relevant information for visible
   * and invisible, but not dont-care predicates.
   */
  private static final class VizInfo
  {
    public final SlotInfo attr;
    public final AttributePredicate predicate;
    public final boolean isVisible;
    
    public VizInfo( final SlotInfo attr, final AttributePredicate predicate,
                    final boolean isVisible )
    {
      this.attr = attr;
      this.predicate = predicate;
      this.isVisible = isVisible;
    }
  }

  
  
  //===========================================================
  //== Convienence Methods
  //===========================================================
  
  // Inherite javadoc
  @Override
  public void setDefaultVisibility( final boolean isVisible )
  {
    final Boolean val = isVisible ? Boolean.TRUE : Boolean.FALSE;
    synchronized( structLock ) {
      defaultViz.setValue( val );
    }
    signalBreak( new AttributeValuesChangedEvent(
		       AbstractPredicateBasedVisibilityView. this,
                       DEFAULT_VISIBILITY, val ) );
  }
  
  // Inherite javadoc
  @Override
  public boolean getDefaultVisibility()
  {
    synchronized( structLock ) {
      return (defaultViz.getValue()).booleanValue();
    }
  }

  
  
  //===========================================================
  //== Rebuild methods
  //===========================================================

  @SuppressWarnings("unchecked")
  @Override
  protected void rebuildModel( final List<ModelEvent> events )
  throws InterruptedException
  {
    synchronized( structLock ) {
      updateVizInfo( events );
      final boolean defaultViz = getDefaultVisibility();
      for( final Iterator<IRNode> nodes = srcModel.getNodes(); nodes.hasNext(); ) {
        /* If another event showed up, give up and try again */
        if( rebuildWorker.isInterrupted() ) throw cannedInterrupt;

        /*
         * Visibility is conceptually from bottom (the end of the sequence of
         * predicates) to the top (first element of the sequence of predicates),
         * with the "default viz" being the initial input into the predicate
         * filters.  The implementation here computes the information from the 
         * first to the last to avoid setting the visibility more that once for
         * any given node.  This is based on the observation that the
         * hightest priority (closest to the start of the sequence) predicate
         * with a non-"pass-through" visibility is the deciding factor on the
         * visibility of a node.
         */
        final IRNode node = nodes.next();
        boolean vizSet = false;
	for( final Iterator<VizInfo> i = vizInfo.iterator(); !vizSet && i.hasNext(); ) {
          final VizInfo info = i.next();
          if( node.valueExists( info.attr ) ) {
            if( info.predicate.includesValue( node.getSlotValue( info.attr ) ) ) {
              visModCore.setVisible( node, info.isVisible );
              vizSet = true;
            }
          }
        }
        if( !vizSet ) visModCore.setVisible( node, defaultViz );
      }
    }

    // Break our views
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  /**
   * If the list of events contains an event from the source predicate model
   * then rebuild the cached predicate information.
   * Caller must hold the structural lock.
   */
  @SuppressWarnings("unchecked")
  private void updateVizInfo( final List<ModelEvent> events )
  {
    boolean rebuildCache = false;
    for( final Iterator<ModelEvent> i = events.iterator(); i.hasNext() && !rebuildCache; ) {
      final ModelEvent e = i.next();
      if( e.getSource() == srcPredModel ) rebuildCache = true;
    }
    
    if( rebuildCache ) {
      vizInfo.clear();

      final SlotInfo<Object> isViz  =
        srcPredModel.getNodeAttribute( PredicateModel.IS_VISIBLE );
      final SlotInfo<AttributePredicate> predSI =
        srcPredModel.getNodeAttribute( PredicateModel.PREDICATE ); 
      final SlotInfo<SlotInfo> attrSI =
        srcPredModel.getNodeAttribute( PredicateModel.ATTRIBUTE ); 

      for( final Iterator<IRNode> i = srcPredModel.getNodes(); i.hasNext(); ) {
        final IRNode n          = i.next(); 
        final Object isNVisible = n.getSlotValue( isViz );
        if( !isNVisible.toString().equals( PredicateModel.LEAVE_ALONE ) ) {
          boolean visibilityAsBoolean;
          if( isNVisible.toString().equals( PredicateModel.PRED_VISIBLE ) ) {
            visibilityAsBoolean = true;
          } else if( isNVisible.toString().equals( PredicateModel.PRED_INVISIBLE ) ) {
            visibilityAsBoolean = false;
          } else {
            throw new FluidError(   "Unknown predicate visibility: \""
                                  + isNVisible + "\"." );
          }
          vizInfo.add(
            new VizInfo( n.getSlotValue( attrSI ),
                         n.getSlotValue( predSI ),
                         visibilityAsBoolean ) );
        }
      }
    }
  }
}
