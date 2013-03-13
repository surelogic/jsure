package edu.cmu.cs.fluid.mvc.tree;

import java.util.Iterator;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.AttributeChangedCallback;
import edu.cmu.cs.fluid.mvc.AttributeManager;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.tree.attributes.MutableSequenceAttributeValueWrapper;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.MutableTreeInterface;
import edu.cmu.cs.fluid.tree.Tree;


/**
 * Concrete core for <code>ForestModel</code>.  Allows the forest
 * to have more than one root.
 *
 * <p>Superclass adds the model-level attribute {@link
 * ForestModel#ROOTS}. 
 *
 * <p>Superclass adds the node-level attributes
 * {@link ForestModel#IS_ROOT}, {@link SymmetricDigraphModel#PARENTS},
 * and {@link ForestModel#LOCATION}.  Adds the attribute 
 * {@link DigraphModel#CHILDREN}.
 *
 * <p>The callback provided to the constructor must handle changes to the
 * sequences in the {@link SymmetricDigraphModel#PARENTS},
 * {@link DigraphModel#CHILDREN}, and {@link ForestModel#ROOTS} attributes.
 *
 * @author Aaron Greenhouse */
public final class ForestForestModelCore
extends ForestModelCore
{
  //===========================================================
  //== Fields
  //===========================================================

  /**
   * The list of the roots of the forest.  Storage for the roots 
   * attribute.
   */
  private final IRSequence<IRNode> rootsSeq;

  @SuppressWarnings("unchecked")
  private Iteratable<IRNode> getRootsIterator() {
    return rootsSeq.elements();
  }

  //===========================================================
  //== Constructor
  //===========================================================

  // Inits model-level attribute ROOTS
  protected ForestForestModelCore(
    final String name, final MutableTreeInterface tree, final IRSequence<IRNode> roots,
    final SlotFactory sf, final boolean mutable, final Model model,
    final Object lock, final AttributeManager manager,
    final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    // Init the tree delegate
    super( name, tree, sf, mutable, model, lock, manager, cb );
    rootsSeq = roots;
    
    /*
     * Use wrapper sequence to trap changes to the sequence of roots.
     * Make sure all the existing elements of the sequence are marked as roots.
     */
    final Iterator<IRNode> rootsEnum = getRootsIterator();
    while( rootsEnum.hasNext() ) {
      final IRNode root = rootsEnum.next();
      root.setSlotValue( isRoot, Boolean.TRUE );
    }
    initializeRoots(
      new MutableSequenceAttributeValueWrapper<IRNode>( 
            structLock, null, rootsSeq, isMutable, 
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
   * Callback for "roots" attribute.  Insures that the model is 
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

    @Override
    public void setElementAt(
      final IRSequence seq, final IRNode parent, final Object elt,
      final Object oldElt )
    {
      /* XXX: Problem --- this update happens in a different critical section
       * XXX: from the one that set the sequence value.
       */
      synchronized( structLock ) {
        ((IRNode)oldElt).setSlotValue( isRoot, Boolean.FALSE );
        ((IRNode)elt).setSlotValue( isRoot, Boolean.TRUE );
      }
      callback.attributeChanged( DigraphModel.CHILDREN, parent, seq );
    }

    @Override
    public void insertElementAt(
      final IRSequence seq, final IRNode parent, final Object elt,
      final InsertionPoint ip )
    {
      /* XXX: Problem --- this update happens in a different critical section
       * XXX: from the one that set the sequence value.
       */
      synchronized( structLock ) {
        ((IRNode)elt).setSlotValue( isRoot, Boolean.TRUE );
      }
      callback.attributeChanged( DigraphModel.CHILDREN, parent, seq );
    }

    @Override
    public void removeElementAt(
      final IRSequence seq, final IRNode parent, final Object oldElt )
    {
      /* XXX: Problem --- this update happens in a different critical section
       * XXX: from the one that set the sequence value.
       */
      synchronized( structLock ) {
        ((IRNode)oldElt).setSlotValue( isRoot, Boolean.FALSE );
      }
      callback.attributeChanged( DigraphModel.CHILDREN, parent, seq );
    }
  }



  //===========================================================
  //== Iterator for ForestModel's getNodes()  
  //===========================================================

  /**
   * Iterator for a forest.  Chains together 
   * the iterators for each rooted tree in the forest.
   */
  private class ForestIterator
  extends AbstractRemovelessIterator<IRNode>
  {
    private final Iterator<IRNode> roots;
    private Iterator<IRNode> current;

    public ForestIterator()
    {
      roots = getRootsIterator();
      advanceCurrent();
    }
  
    private void advanceCurrent()
    {
      if( roots.hasNext() ) {
        final IRNode root = roots.next();
        current = forest.topDown( root );
	
      } else {
        current = null;
      } 
    }

    @Override
    public boolean hasNext()
    {
      return (current != null);
    }

    @Override
    public IRNode next()
    {
      final IRNode o = current.next();
      if( !current.hasNext() ) advanceCurrent();
      return o;
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
    return new ForestIterator();
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
    final Iterator roots = getRootsIterator();
    while( roots.hasNext() ) {
      final IRNode root = (IRNode)roots.next();
      root.setSlotValue( isRoot, Boolean.FALSE );

//      if (CORE.isDebugEnabled()) {
//        CORE.debug("Set to not root: "+root);
//      }
    }

    final int oldSize = rootsSeq.size();
    if( oldSize > 0 ) {
      for( int i = 0; i < oldSize; i++ ) {
        // CORE.debug("Removing root "+rootsSeq.location(0));
        rootsSeq.removeElementAt( rootsSeq.location( 0 ) );
      }
    }

    precomputedNodes = false;
  }

  /** Test if a node is a root in the forest. */
  @Override
  public boolean isRoot( final IRNode node )
  {
    final Boolean val = node.getSlotValue( isRoot );
    return val.booleanValue();
  }

  @Override
  public void removeRoot( final IRNode root ) {
    if (!isRoot(root)) {
      throw new IllegalArgumentException( root + " is not a root!" );
    }
    for(int i=rootsSeq.size()-1; i>=0; i--) {
      if (root.equals(rootsSeq.elementAt(i))) {
        rootsSeq.removeElementAt(rootsSeq.location(i));
      }
    }
    root.setSlotValue( isRoot, Boolean.FALSE );
  } 
    
  /**
   * Append a root to the forest.
   * @exception IllegalArgumentException Thrown if the node is already
   * a member of the forest.
   */
  @Override
  public void addRoot( final IRNode root )
  {
    if( isRoot( root ) ) { 
      throw new IllegalArgumentException( root + " is already a root!" );
    }
    InsertionPoint ip = new InsertionPoint( rootsSeq.lastLocation(), false );
    ip.insert( rootsSeq, root );
    root.setSlotValue( isRoot, Boolean.TRUE );
  }

  /**
   * Insert a root to the forest at the beginning of the roots.
   * @exception IllegalArgumentException Thrown if the node is already
   * a member of the forest.
   */
  @Override
  public void insertRoot( final IRNode root )
  {
    if( isRoot( root ) ) { 
      throw new IllegalArgumentException( root + " is already a root!" );
    }
    InsertionPoint ip = new InsertionPoint( rootsSeq.firstLocation(), true );
    ip.insert( rootsSeq, root );
    root.setSlotValue( isRoot, Boolean.TRUE );
  }

  /**
   * Insert a new root before another root.
   * @exception IllegalArgumentException Thrown if the new root is already
   * a member of the forest.
   */
  @Override
  public void insertRootBefore( final IRNode newRoot, final IRNode root )
  {
    IRLocation loc = null;
    for( int i = 0; (loc == null) && (i < rootsSeq.size()); i++ ) {
      final IRNode node = rootsSeq.elementAt( i );
      if( newRoot.equals(node) ) {
        throw new IllegalArgumentException( newRoot + " is already a root!" );
      }
      if( root.equals(node) ) loc = rootsSeq.location( i );
    }
    if( loc == null ) {
      throw new IllegalArgumentException( root + " is not a root!" );
    }
    
    rootsSeq.insertElementBefore( newRoot, loc );
    newRoot.setSlotValue( isRoot, Boolean.TRUE );
  }

  /**
   * Insert a new root after another root.
   * @exception IllegalArgumentException Thrown if the new root is already
   * a member of the forest.
   */
  @Override
  public void insertRootAfter( final IRNode newRoot, final IRNode root )
  {
    IRLocation loc = null;
    for( int i = 0; (loc == null) && (i < rootsSeq.size()); i++ ) {
      final IRNode node = rootsSeq.elementAt( i );
      if( newRoot.equals(node) ) {
        throw new IllegalArgumentException( newRoot + " is already a root!" );
      }
      if( root.equals(node) ) loc = rootsSeq.location( i );
    }
    if( loc == null ) {
      throw new IllegalArgumentException( root + " is not a root!" );
    }
    
    rootsSeq.insertElementAfter( newRoot, loc );
    newRoot.setSlotValue( isRoot, Boolean.TRUE );
  }

  /**
   * Insert a new root at a given location.  The root
   * is inserted using an insertion point before the given location.
   */
  @Override
  public void insertRootAt( final IRNode root, final IRLocation loc )
  {
    if( isRoot( root ) ) { 
      throw new IllegalArgumentException( root + " is already a root!" );
    }
    rootsSeq.insertElementBefore( root, loc );
    root.setSlotValue( isRoot, Boolean.TRUE );
  }

  /**
   * Insert a new root with a given insertion point.
   */
  @Override
  public void insertRootAt( final IRNode root, final InsertionPoint ip )
  {
    if( isRoot( root ) ) { 
      throw new IllegalArgumentException( root + " is already a root!" );
    }
    ip.insert( rootsSeq, root );
    root.setSlotValue( isRoot, Boolean.TRUE );
  }

  /**
   * Get the roots of the forest in order.
   */
  @Override
  public Iteratable<IRNode> getRoots()
  {
    return getRootsIterator();
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
    protected final SlotFactory slotFactory;
    protected final boolean isMutable;

    public StandardFactory( final SlotFactory sf, final boolean mutable )
    {
      slotFactory = sf;
      isMutable = mutable;
    }

    @Override
    public ForestModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      final Tree tree = new Tree( name + "-tree_delegate", slotFactory );
      final IRSequence<IRNode> roots = slotFactory.newSequence(~0);

      ForestModelCore fmc =
        new ForestForestModelCore(
              name, tree, roots, slotFactory, isMutable,
              model, structLock, manager, cb );
      tree.saveAttributes(fmc.b);
      return fmc;
    }
  }
  
  public static class DelegatingFactory
  implements Factory
  {
    protected final MutableTreeInterface tree;
    protected final IRSequence<IRNode> roots;
    protected final boolean isMutable;
    protected final SlotFactory slotFactory;

    public DelegatingFactory(
      final MutableTreeInterface t, final IRSequence<IRNode> rts, final SlotFactory sf,
      final boolean mutable )
    {
      tree = t;
      roots = rts;
      slotFactory = sf; 
      isMutable = mutable;
    }

    @Override
    public ForestModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager,
      final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new ForestForestModelCore(
                   name, tree, roots, slotFactory, isMutable,
                   model, structLock, manager, cb );
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}


