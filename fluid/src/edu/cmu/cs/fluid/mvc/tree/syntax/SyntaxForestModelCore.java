package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.mvc.AbstractCore;
import edu.cmu.cs.fluid.mvc.AttributeManager;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.DerivedSlotInfo;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.tree.*;

/**
 * Abstract implemenation of core class for <code>SyntaxForestModel</code>.
 * <p>Adds the node-level attribute {@link SyntaxForestModel#OPERATOR}.  The
 * subclass must init the value of this attribute.
 *
 * <p>The callback provided to the constructor must handle changes to the
 * sequences in the {@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS} attribute.
 *
 * @author Edwin Chan
 * @author Aaron Greenhouse
 */
public final class SyntaxForestModelCore
extends AbstractCore
{
  //===========================================================
  //== Fields
  //===========================================================

  /** The operator attribute */
  protected final SlotInfo<Operator> operators;

  /**
   * The underlying Syntax tree.  This will be shared with a ForestModelCore
   * that will see it only as a {@link edu.cmu.cs.fluid.tree.Tree}.
   */
  protected final SyntaxTreeInterface syntaxTree; 

  
  
  //===========================================================
  //== Constructor
  //===========================================================


  protected SyntaxForestModelCore(
    final String name, final Model model, final Object lock,
    final AttributeManager manager, final SyntaxTreeInterface tree )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );
    syntaxTree = tree;

    // Init node attributes
    operators = new OperatorSlotInfo( name + "-" + SyntaxForestModel.OPERATOR );
    attrManager.addNodeAttribute(
      SyntaxForestModel.OPERATOR, Model.STRUCTURAL, operators );
  }

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Inner Classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Implementation of the {@link SyntaxForestModel#OPERATOR} attribute
   * used by {@link SyntaxForestModelCore}.  Delegates to the underlying
   * {@link edu.cmu.cs.fluid.tree.SyntaxTree}.
   */
  protected class OperatorSlotInfo
  extends DerivedSlotInfo<Operator>
  {
    public OperatorSlotInfo( final String name )
    throws SlotAlreadyRegisteredException
    {
      super( name, IROperatorType.prototype );
    }

    /*
     * NOTE: setSlotValue() should never be called because instances of this
     * class are wrapped up again by the AttributeManager, and they are 
     * always wrapped to be immutable (see the constructor of
     * SyntaxForestModelCore).
     *
     * Also, the methods below do not need to synchronized on the structural
     * lock because the wrappers provided the attribute manager take care of
     * that.
     */
    
    @Override
    protected Operator getSlotValue( final IRNode node ) {
      return syntaxTree.getOperator( node );
    }

    /** This value always exists */
    @Override
    protected boolean valueExists( final IRNode node ) {
      return syntaxTree.opExists( node );
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End of Inner Classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin SyntaxTree
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public Operator getOperator( final IRNode n ) {
    return syntaxTree.getOperator(n);
  }

  public boolean opExists( final IRNode n ) {
    return syntaxTree.opExists(n);
  }

  /** Add a node to the directed graph.
   * Notify define observers and inform listeners of this new node.
   * @param n a new node to add to the graph
   * @throws SlotImmutableException if node already in graph
   */    
  public void initNode( final IRNode n, Operator op )
  {
    syntaxTree.initNode( n, op );
    clearNode(n, op.numChildren());
  }

  public void initNode( final IRNode n, Operator op, int min )
  {
    syntaxTree.initNode( n, op, min );
    clearNode(n, min);
  }

  public void initNode( final IRNode n, Operator op, IRNode[] children )
  {
    syntaxTree.initNode( n, op, children );
  }

  protected void clearNode ( final IRNode n, int i ) {
    for (i--; i>=0; i--) {
      syntaxTree.setChild( n, i, null);
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public static interface Factory
  {
    public SyntaxForestModelCore create(
      String name, Model model, Object structLock, AttributeManager manager )
    throws SlotAlreadyRegisteredException;
  }
  
  /*
   * Only have a delegate-taking factory because the tree must be shared
   * with a ForestModelCore.
   */
  
  public static class StandardFactory
  implements Factory
  {
    private final SyntaxTreeInterface syntaxTree;
    
    public StandardFactory( final SyntaxTreeInterface tree )
    {
      syntaxTree = tree;
    }
    
    @Override
    public SyntaxForestModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager )
    throws SlotAlreadyRegisteredException
    {
      return new SyntaxForestModelCore(
                   name, model, structLock, manager, syntaxTree );
    }
  }
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
