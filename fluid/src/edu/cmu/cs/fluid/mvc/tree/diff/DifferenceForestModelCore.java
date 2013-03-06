package edu.cmu.cs.fluid.mvc.tree.diff;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.*;

/**
 * Core implemenation of the {#link DifferenceForestModel} interface.
 * <em>Does not maintain the {@link DifferenceForestModel#NODE_ATTR_SRC}
 * or {@link DifferenceForestModel#DIFF_LOCAL} attributes.</em>
 */
public final class DifferenceForestModelCore
extends AbstractCore
{
  //===========================================================
  //== Fields
  //===========================================================

  /**
   * Storage for the {@link DifferenceForestModel#POSITION_ENUM}
   * enumeration.
   */
  private static final IREnumeratedType diffPosition =
    newEnumType( DifferenceForestModel.POSITION_ENUM,
                 new String[] { DifferenceForestModel.POSITION_MOVED,
                                DifferenceForestModel.POSITION_NA,
                                DifferenceForestModel.POSITION_ANCESTOR,
                                DifferenceForestModel.POSITION_SAME } );

  /**
   * Storage for the {@link DifferenceForestModel#SUBTREE_ENUM}
   * enumeration.
   */
  private static final IREnumeratedType diffSubtree =
    newEnumType( DifferenceForestModel.SUBTREE_ENUM,
                 new String[] { DifferenceForestModel.SUBTREE_SAME,
                                DifferenceForestModel.SUBTREE_CHANGED,
                                DifferenceForestModel.SUBTREE_NA } );

  /**
   * Array of elements of the {@link DifferenceForestModel#POSITION_ENUM}
   * enumeration, in order.
   */
  private static final IREnumeratedType.Element[] positionElts = {
    diffPosition.getElement( DifferenceForestModel.POS_MOVED ),
    diffPosition.getElement( DifferenceForestModel.POS_NA ),
    diffPosition.getElement( DifferenceForestModel.POS_ANC ),
    diffPosition.getElement( DifferenceForestModel.POS_SAME )
  };

  /**
   * Array of elements of the {@link DifferenceForestModel#SUBTREE_ENUM}
   * enumeration, in order.
   */
  private static final IREnumeratedType.Element[] subtreeElts = {
    diffSubtree.getElement( DifferenceForestModel.SUB_SAME ),
    diffSubtree.getElement( DifferenceForestModel.SUB_DIFF ),
    diffSubtree.getElement( DifferenceForestModel.SUB_NA )
  };

  /**
   * Attribute storage for the {@link DifferenceForestModel#DIFF_POSITION}
   * attribute.
   */
  private final SlotInfo<IREnumeratedType.Element> position;

  /**
   * Attribute storage for the {@link DifferenceForestModel#DIFF_SUBTREE}
   * attribute.
   */
  private final SlotInfo<IREnumeratedType.Element> subtree;

  /**
   * Attribute storage for the {@link DifferenceForestModel#DIFF_LABEL}
   * attribute.
   */
  private final SlotInfo<IRNode> label;

  /**
   * Attribute storage for the {@link DifferenceForestModel#DEFAULT_ATTR_SRC}
   * attribute.
   */
  private final ComponentSlot<Boolean> compSelector;
  
  

  //===========================================================
  //== Constructor
  //===========================================================

  protected DifferenceForestModelCore(
    final String name, final Model model, final Object lock,
    final AttributeManager manager, AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );

    final ExplicitSlotFactory sf = SimpleExplicitSlotFactory.prototype;
    position = sf.newAttribute(
                 name + "-" + DifferenceForestModel.DIFF_POSITION,
                 diffPosition, positionElts[DifferenceForestModel.POS_SAME] );
    subtree = sf.newAttribute(
                name + "-" + DifferenceForestModel.DIFF_SUBTREE, diffSubtree,
		subtreeElts[DifferenceForestModel.SUB_SAME] );
    label = sf.newAttribute(
              name + "-" + DifferenceForestModel.DIFF_LABEL,
              IRNodeType.prototype );

    attrManager.addNodeAttribute(
      DifferenceForestModel.DIFF_POSITION, Model.INFORMATIONAL, position );
    attrManager.addNodeAttribute(
      DifferenceForestModel.DIFF_SUBTREE, Model.INFORMATIONAL, subtree );
    attrManager.addNodeAttribute(
      DifferenceForestModel.DIFF_LABEL, Model.INFORMATIONAL, label );

    compSelector = new SimpleComponentSlot<Boolean>(
                         IRBooleanType.prototype, sf, Boolean.FALSE );

    attrManager.addCompAttribute(
      DifferenceForestModel.DEFAULT_ATTR_SRC, Model.INFORMATIONAL,
      compSelector, cb );
  }
  
  

  //===========================================================
  //== Methods to get the enumerations and their elements
  //===========================================================

  /** Get the {@link DifferenceForestModel#POSITION_ENUM} enumeration. */
  public static IREnumeratedType getDiffPositionEnum()
  {
    return diffPosition;
  }
  
  /**
   * Get the elements of the {@link DifferenceForestModel#POSITION_ENUM}
   * enumeration in order.
   */
  public static IREnumeratedType.Element[] getDiffPositionElts()
  {
    return positionElts;
  }

  /** Get the {@link DifferenceForestModel#SUBTREE_ENUM} enumeration. */
  public static IREnumeratedType getDiffSubtreeEnum()
  {
    return diffSubtree;
  }
  
  /**
   * Get the elements of the {@link DifferenceForestModel#SUBTREE_ENUM}
   * enumeration in order.
   */
  public static IREnumeratedType.Element[] getDiffSubtreeElts()
  {
    return subtreeElts;
  }
  
  
  //===========================================================
  //== Attribute getters and setters.  Setters should not be
  //== exposed by a public interface of a model.  They are for 
  //== the use of model implementations only.
  //===========================================================

  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_POSITION} attribute.
   */
  public IREnumeratedType.Element getDiffPosition( final IRNode node )
  {
    return node.getSlotValue( position );
  }

  /**
   * Set the value of the {@link DifferenceForestModel#DIFF_POSITION} attribute.
   */
  public void setDiffPosition(
    final IRNode node, final IREnumeratedType.Element val )
  {
    node.setSlotValue( position, val );
  }

  /**
   * Set the value of the {@link DifferenceForestModel#DIFF_POSITION} attribute
   * using the index of the enumeration element.
   */
  public void setDiffPosition( final IRNode node, final int valIdx )
  {
    node.setSlotValue( position, positionElts[valIdx] );
  }

  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_SUBTREE} attribute.
   */
  public IREnumeratedType.Element getDiffSubtree( final IRNode node )
  {
    return node.getSlotValue( subtree );
  }

  /**
   * Set the value of the {@link DifferenceForestModel#DIFF_SUBTREE} attribute.
   */
  public void setDiffSubtree(
    final IRNode node, final IREnumeratedType.Element val )
  {
    node.setSlotValue( subtree, val );
  }

  /**
   * Set the value of the {@link DifferenceForestModel#DIFF_SUBTREE} attribute
   * using the index of the enumeration element.
   */
  public void setDiffSubtree( final IRNode node, final int valIdx )
  {
    node.setSlotValue( subtree, subtreeElts[valIdx] );
  }

  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_LABEL} attribute.
   */
  public IRNode getDiffLabel( final IRNode node )
  {
    return node.getSlotValue( label );
  }

  /**
   * Set the value of the {@link DifferenceForestModel#DIFF_LABEL} attribute.
   */
  public void setDiffLabel( final IRNode node, final IRNode val )
  {
    node.setSlotValue( label, val );
  }
  
  /**
   * Get the value of the {@link DifferenceForestModel#DEFAULT_ATTR_SRC} attribute.
   */
  public boolean getCompSelector()
  {
    return compSelector.getValue().booleanValue();
  }
  
  /**
   * Set the value of the {@link DifferenceForestModel#DEFAULT_ATTR_SRC} attribute.
   */
  public void setCompSelector( final boolean val )
  {
    compSelector.setValue( val ? Boolean.TRUE : Boolean.FALSE );
  }

  
  
  //===========================================================
  //== Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public DifferenceForestModelCore create(
      String name, Model model, Object structLock, AttributeManager manager,
      AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }
  
  private static class StandardFactory
  implements Factory
  {
    @Override
    public DifferenceForestModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new DifferenceForestModelCore( name, model, structLock, manager, cb );
    }
  }

  public static final Factory standardFactory = new StandardFactory();
}
