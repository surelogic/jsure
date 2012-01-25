package edu.cmu.cs.fluid.mvc.tree;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.AttributeChangedCallback;
import edu.cmu.cs.fluid.mvc.AttributeManager;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.tree.attributes.MutableSequenceAttributeValueWrapper;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.MutableTreeInterface;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.SingletonIterator;

/**
 * Concrete core for <code>ForestModel</code>.  Allows the forest
 * to have no more than one root.
 *
 * <p>Superclass adds the model-level attribute {@link
 * ForestModel#ROOTS}.  <p>Superclass adds the node-level attributes
 * {@link ForestModel#IS_ROOT}, {@link SymmetricDigraphModel#PARENTS},
 * and {@link ForestModel#LOCATION}.  Adds the attribute
 * {@link DigraphModel#CHILDREN}.
 *
 * <p>The callback provided to the constructor must handle changes to the
 * sequences in the {@link SymmetricDigraphModel#PARENTS},
 * {@link DigraphModel#CHILDREN}, and {@link ForestModel#ROOTS} attributes.
 *
 * @author Aaron Greenhouse */
public final class TreeForestModelCore
extends ForestModelCore
{
  //===========================================================
  //== Fields
  //===========================================================
  
  /**
   * The roots sequence.  A 1-element sequence
   */
  private final IRSequence<IRNode> roots;



  //===========================================================
  //== Constructor
  //===========================================================

  // Inits model-level attribute ROOTS
  // urRoot must already be part of tree (ie, must be initNoded)
  protected TreeForestModelCore(
    final String name, final MutableTreeInterface tree, final SlotFactory sf,
    final boolean mutable, final Model model, final Object lock,
    final AttributeManager manager, final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    // Init the tree delegate
    super( name, tree, sf, mutable, model, lock, manager, cb );

    roots = sf.newSequence( 1 );    
    roots.setElementAt( null, 0 );
    initializeRoots( new MutableSequenceAttributeValueWrapper<IRNode>(
                           structLock, null, roots, mutable,
                           new RootsSequenceChangedCallback( cb ) ) );
  }

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Inner Classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  
  //===========================================================
  //== Sequence callback for the "children" attribute
  //===========================================================
  
  /**
   * Callback for "children" attribute.  Insures that the model is 
   * broken, and alters the isRoot property when appropriate.
   */
  private class RootsSequenceChangedCallback
  implements MutableSequenceAttributeValueWrapper.Callback
  {
    final AttributeChangedCallback callback;

    public RootsSequenceChangedCallback( final AttributeChangedCallback cb )
    {
      callback = cb;
    }

    public void setElementAt(
      final IRSequence seq, final IRNode parent, final Object elt,
      final Object oldElt )
    {
      /* XXX: Problem --- this update happens in a different critical section
       * XXX: from the one that set the sequence value.
       */
      synchronized( structLock ) {
        if( oldElt != null ) {
          ((IRNode)oldElt).setSlotValue( isRoot, Boolean.FALSE );
        }
        if( elt != null ) ((IRNode)elt).setSlotValue( isRoot, Boolean.TRUE );
        callback.attributeChanged( DigraphModel.CHILDREN, parent, seq );
      }
    }

    public void insertElementAt(
      final IRSequence seq, final IRNode parent, final Object elt,
      final InsertionPoint ip )
    {
      /* XXX: Problem --- this update happens in a different critical section
       * XXX: from the one that set the sequence value.
       */
      synchronized( structLock ) {
        if( elt != null ) ((IRNode)elt).setSlotValue( isRoot, Boolean.TRUE );
        callback.attributeChanged( DigraphModel.CHILDREN, parent, seq );
      }
    }

    public void removeElementAt(
      final IRSequence seq, final IRNode parent, final Object oldElt )
    {
      /* XXX: Problem --- this update happens in a different critical section
       * XXX: from the one that set the sequence value.
       */
      synchronized( structLock ) {
        if( oldElt != null ) {
          ((IRNode)oldElt).setSlotValue( isRoot, Boolean.FALSE );
        }
        callback.attributeChanged( DigraphModel.CHILDREN, parent, seq );
      }
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End of Inner Classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Node Methods
  //===========================================================

  @Override
  public Iterator<IRNode> getNodes()
  {
    final IRNode root = roots.elementAt( 0 );
    if( root != null ) {
      return forest.topDown( root );
    } else {
      return new EmptyIterator<IRNode>();
    }
  }

  // Need to implement this for real
  // for now always throw UnsupportedOperationExceptoin
  @Override
  public void addNode( final IRNode node, final AVPair[] vals )
  {
    throw new UnsupportedOperationException( "Not yet implemented" );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Forest Model Methods
  //===========================================================

  /** Clear the tree */
  @Override
  public void clearForest()
  {
    final IRNode root = roots.elementAt( 0 );
    if( root != null ) {
      root.setSlotValue( isRoot, Boolean.FALSE );
      forest.removeChildren( root );
      roots.setElementAt( null, 0 );
    }

    precomputedNodes = false;
  }

  /** Test if a node is a root in the forest. */
  @Override
  public boolean isRoot( final IRNode node )
  {
    return (node != null) && (node == roots.elementAt( 0 ));
  }

  @Override
  public void removeRoot( final IRNode root ) {
    final IRNode oldRoot = roots.elementAt( 0 );
    if( oldRoot == null ) {
      throw new UnsupportedOperationException( "Tree has no root!" );
    } else if( !oldRoot.equals(root) ) {
      throw new IllegalArgumentException( root + " is not a root!" );
    }
    roots.setElementAt(null, 0);
  }

  /**
   * Append a root to the forest.
   * @exception IllegalArgumentException Thrown if the node is already
   * a member of the forest.
   */
  @Override
  public void addRoot( final IRNode root )
  {
    final IRNode oldRoot = roots.elementAt( 0 );
    if( root == null ) {
      throw new IllegalArgumentException( root + " is null!" );
    }
    if( root.equals(oldRoot) ) {
      throw new IllegalArgumentException( root + " is already a root!" );
    }
    if( oldRoot != null ) {
      throw new UnsupportedOperationException( "Tree already has a root!" );
    } else {
      roots.setElementAt( root, 0 );
      root.setSlotValue( isRoot, Boolean.TRUE );
    }
  }

  /**
   * Insert a root at the start of the forest.
   * @exception IllegalArgumentException Thrown if the node is already
   * a member of the forest.
   */
  @Override
  public void insertRoot( final IRNode root )
  {
    addRoot( root );
  }

  /**
   * Insert a new root before another root.
   * @exception IllegalArgumentException Thrown if the new root is already
   * a member of the forest.
   */
  @Override
  public void insertRootBefore( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot insert a root into a tree." );
  }

  /**
   * Insert a new root after another root.
   * @exception IllegalArgumentException Thrown if the new root is already
   * a member of the forest.
   */
  @Override
  public void insertRootAfter( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot insert a root into a tree." );
  }

  /**
   * Insert a new root at a given location.  The root
   * is inserted using an insertion point before the given location.
   */
  @Override
  public void insertRootAt( final IRNode root, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Cannot insert a root into a tree." );
  }

  /**
   * Insert a new root with a given insertion point
   */
  @Override
  public void insertRootAt( final IRNode root, final InsertionPoint ip )
  {
    throw new UnsupportedOperationException( "Cannot insert a root into a tree." );
  }

  /**
   * Get the roots of the forest in order.
   */
  @Override
  public Iteratable<IRNode> getRoots()
  {
    final IRNode root = roots.elementAt( 0 );
    if( root == null )
      return new EmptyIterator<IRNode>();
    else return new SingletonIterator<IRNode>( root );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  
  public static class StandardFactory
  implements Factory
  {
    private final SlotFactory slotFactory;
    private final boolean isMutable;

    public StandardFactory( final SlotFactory sf, final boolean mutable )
    {
      slotFactory = sf;
      isMutable = mutable;
    }

    public ForestModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      final Tree tree = new Tree( name + "-tree_delegate", slotFactory );
      ForestModelCore fmc =
        new TreeForestModelCore( name, tree, slotFactory, isMutable, model,
				 structLock, manager, cb ); 
      tree.saveAttributes(fmc.b);
      return fmc;
    }
  }
  
  public static class DelegatingFactory
  implements Factory
  {
    private final MutableTreeInterface tree;
    private final SlotFactory slotFactory;
    private final boolean isMutable;

    public DelegatingFactory(
      final MutableTreeInterface t, final SlotFactory sf, final boolean mutable )
    {
      tree = t;
      slotFactory = sf;
      isMutable = mutable;
    }

    public ForestModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new TreeForestModelCore( name, tree, slotFactory, isMutable,
                                      model, structLock, manager, cb );
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}


