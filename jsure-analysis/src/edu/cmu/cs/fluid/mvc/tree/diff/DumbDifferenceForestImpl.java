/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/diff/DumbDifferenceForestImpl.java,v 1.8 2007/07/05 18:15:25 aarong Exp $
 *
 * DumbDifferenceForestImpl.java
 * Created on April 26, 2002, 4:12 PM
 */

package edu.cmu.cs.fluid.mvc.tree.diff;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.diff.DifferenceModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.tree.ForestModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * Implementation of {@link DumbDifferenceForest}.
 */
final class DumbDifferenceForestImpl
extends AbstractDifferenceForestModel
implements DumbDifferenceForest
{
  //===========================================================
  //== Fields
  //===========================================================

  /**
   * Storage for the {@link DumbDifferenceForest#LOCAL_ENUM}
   * enumeration.
   */
  private static final IREnumeratedType diffLocal =
    AbstractCore.newEnumType(
      DumbDifferenceForest.LOCAL_ENUM,
      new String[] { DifferenceForestModel.NODE_ADDED,
                     DifferenceForestModel.NODE_DELETED,
                     DifferenceForestModel.NODE_PHANTOM,
                     DifferenceForestModel.NODE_SAME,
                     DifferenceForestModel.NODE_CHANGED,
                     DumbDifferenceForest.NODE_DIFFERENT } );

  /**
   * Array of elements of the {@link DumbDifferenceForest#LOCAL_ENUM}
   * enumeration, in order.
   */
  protected static final IREnumeratedType.Element[] localElts = {
    diffLocal.getElement( DifferenceForestModel.ADDED ),
    diffLocal.getElement( DifferenceForestModel.DELETED ),
    diffLocal.getElement( DifferenceForestModel.PHANTOM ),
    diffLocal.getElement( DifferenceForestModel.SAME ),
    diffLocal.getElement( DifferenceForestModel.CHANGED ),
    diffLocal.getElement( DumbDifferenceForest.DIFFERENT )
  };
  
  /**
   * Attribute storage for the {@link DifferenceForestModel#DIFF_LOCAL}
   * attribute.
   */
  private final SlotInfo<IREnumeratedType.Element> local;

  /**
   * Attribute storage for the {@link DifferenceForestModel#NODE_ATTR_SRC}
   * attribute
   */
  private final SlotInfo<Boolean> nodeSelector;
  
  

  public DumbDifferenceForestImpl(
    final String name, final ForestModel base, final ForestModel delta,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf, final DifferenceModelCore.Factory dmf,
    final DifferenceForestModelCore.Factory dfmf,
    final AttributeMergingManager.Factory mergFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, base, delta, mf, vf, fmf, dmf, dfmf, mergFactory );
    local = SimpleSlotFactory.prototype.newAttribute(
              name + "-" + DIFF_LOCAL, diffLocal );
    attrManager.addNodeAttribute( DIFF_LOCAL, Model.INFORMATIONAL, local );
    
    nodeSelector = new NodeSelectorAttribute(
                         name + "-" + NODE_ATTR_SRC, this, local,
                         attrManager.getCompAttribute( DEFAULT_ATTR_SRC ) );
    attrManager.addNodeAttribute(
      NODE_ATTR_SRC, Model.INFORMATIONAL, nodeSelector );
    
    mergeAttributes( AttributeChangedCallback.nullCallback );
    rebuildModel();
    finalizeInitialization();
  }

  
  
  private final static class NodeSelectorAttribute
  extends DerivedSlotInfo<Boolean>
  {
    final Model model;
    final ComponentSlot<Boolean> compSelector;
    final SlotInfo<IREnumeratedType.Element> localDiff;
    
    public NodeSelectorAttribute(
      final String name, final Model m, final SlotInfo<IREnumeratedType.Element> local,
      final ComponentSlot<Boolean> selector )
    throws SlotAlreadyRegisteredException
    {
      super( name, IRBooleanType.prototype );
      model = m;
      localDiff = local;
      compSelector = selector;
    }
    
    @Override
    protected Boolean getSlotValue( final IRNode node )
    {
      final IREnumeratedType.Element val = 
        node.getSlotValue( localDiff );

      Boolean select = null;
      if( val == localElts[ADDED] ) {
        select = Boolean.FALSE;
      } else if(    (val == localElts[DELETED])
                 || (val == localElts[PHANTOM]) ) {
        select = Boolean.TRUE;
      } else {
        select = compSelector.getValue();
      }
      return select;
    }
    
    @Override
    protected boolean valueExists( final IRNode node ) 
    {
      return model.isAttributable( node, NODE_ATTR_SRC );
    }
  }
  

  
  /**
   * Get the {@link DifferenceForestModel#DIFF_LOCAL} attribute value.
   */
  @Override
  public IREnumeratedType.Element getDiffLocal(final IRNode node)
  {
    synchronized( structLock ) {
      return node.getSlotValue( local );
    }
  }
  
  /**
   * Clear the state needed for maintaining local difference information.
   */
  @Override
  protected void initLocalDiff()
  {
    // noop
  }
  
  /**
   * Returns whether the model considers the given node to be a leaf node.
   */
  @Override
  protected boolean isLeaf( final IRNode n )
  {
    return (baseForest.numChildren( n ) == 0);
  }
  
  /**
   * Get the local difference enumeration element of the given index.
   */
  @Override
  protected IREnumeratedType.Element localElts( final int idx )
  {
    return localElts[idx];
  }
  
  /**
   * Invoked on preserved nodes to determine how the local state of the
   * node may have changed.
   */
  @Override
  protected void markLocalDiff( final IRNode n )
  {
    int tag = SAME;
    final Iterator baseAttrs = baseForest.getNodeAttributes();
    while( (tag == SAME) && baseAttrs.hasNext() ) {
      final String attr = (String) baseAttrs.next();
      if(    (baseForest.getNodeAttrKind( attr ) == Model.INFORMATIONAL)
          && deltaForest.isNodeAttribute( attr ) ) {
        final Object before = n.getSlotValue( baseForest.getNodeAttribute( attr ) );
        final Object after = n.getSlotValue( deltaForest.getNodeAttribute( attr ) );
        if( !before.equals( after ) ) {
          tag = DIFFERENT;
          changed.add( n );
        }
      }
    }
    n.setSlotValue( local, localElts[tag] );
  }
  
  /**
   * Query if the children of a node are ordered.  We have to work on 
   * all forests, so always return <code>true</code>.
   */
  @Override
  protected boolean nodeOrderedP( final IRNode n )
  {
    return true;
  }
  
  /**
   * Set the {@link DifferenceForestModel#DIFF_LOCAL} attribute for the
   * given using the local difference enumeration item identified by
   * the given index.
   */
  @Override
  protected void setDiffLocal( final IRNode node, final int valIdx )
  {
    node.setSlotValue( local, localElts[valIdx] );
  }
  
  /**
   * Called by {@link #buildDiffTree} to update any local difference
   * state needed for building the exported model.
   * @param n ???
   * @param add ???
   */
  @Override
  protected void updateModelSpecificVectors( final IRNode n, final IRNode add )
  {
    // noop?
  }
  
  /**
   * Get the value of the {@link DifferenceForestModel#NODE_ATTR_SRC} attribute.
   */
  @Override
  public boolean getNodeSelector( final IRNode node )
  {
    synchronized( structLock ) {
      return node.getSlotValue( nodeSelector ).booleanValue();
    }
  }
  
}
