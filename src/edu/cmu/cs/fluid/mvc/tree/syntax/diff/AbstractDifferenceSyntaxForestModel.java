package edu.cmu.cs.fluid.mvc.tree.syntax.diff;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.AttributeMergingManager;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.diff.DifferenceModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModelCore;
import edu.cmu.cs.fluid.mvc.tree.diff.AbstractDifferenceForestModel;
import edu.cmu.cs.fluid.mvc.tree.diff.DifferenceForestModelCore;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModel;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModelCore;
import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Generic implementation of a DifferenceSyntaxForestModel that views two
 * syntax forests and exports a syntax forest that is their difference.
 *
 * @author Edwin Chan 
 */
public abstract class AbstractDifferenceSyntaxForestModel
extends AbstractDifferenceForestModel
{
  /** SyntaxForestModelCore delegate */
  protected final SyntaxForestModelCore synForestModCore;
  
  /** Base model as a syntax forest */
  protected final SyntaxForestModel baseS;
  
  /** Delta model as a syntax forest */
  protected final SyntaxForestModel deltaS;
  
  /** Addtional differencing state? */
  protected final Set<IRNode> nulled = new HashSet<IRNode>();

  
  
  /// Constructor
  protected AbstractDifferenceSyntaxForestModel(
    final String name, final SyntaxForestModel base, final SyntaxForestModel delta,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf, final DifferenceModelCore.Factory dmf,
    final DifferenceForestModelCore.Factory dfmf,
    final SyntaxForestModelCore.Factory sfmc,
    final AttributeMergingManager.Factory mergFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, base, delta, mf, vf, fmf, dmf, dfmf, mergFactory );
    synForestModCore = sfmc.create( name, this, structLock, attrManager );
    baseS = base;
    deltaS = delta;
  }

  
  
  /**
   * Get the operator associated with the given node.  If both the
   * base and the delta model have an operator they had better be the same
   * operator.  Otherwise it returns the non-<code>null</code> operator of 
   * pair.  
   * @throws FluidError Thrown in the base and delta models both have 
   *    non-<code>null</code> operators for the given node but they aren't
   *    equal (by <code>==</code>).
   */
  protected final Operator getOperatorView( final IRNode n )
  {
    Operator op1 = baseS.opExists(n) ? baseS.getOperator(n) : null;
    Operator op2 = deltaS.opExists(n) ? deltaS.getOperator(n) : null;
    if (op1 == op2) {
      return op1; 
    } else if (op1 == null) {
      return op2;
    } else if (op2 == null) {
      return op1;
    }
    throw new FluidError(op1+" is not the same as "+op2);
  }

  
  
  /*
   * AbstractDifferenceForestModel methods.
   */
  
  /** 
   * (Re)initializes the node for this model, so we can use it (again).
   */
  @Override
  protected void setupNode(IRNode n) {
    Operator op = getOperatorView(n);

    // add node to tree.  Must only call initNode once!
    if( !forestModCore.isNode( n ) ) {
      synForestModCore.initNode( n, op );
    }
    
    /* Make sure all the children are null */
    forestModCore.removeChildren( n );
    final int nulChildren = op.numChildren();
    for( int i = 0; i < nulChildren; i++ ) {
      forestModCore.setChild( n, i, null );
    }
    nulled.add(n);
  }

  @Override
  protected void initLocalDiff()
  {
    nulled.clear();
  }


  
  /*
   * Stuff from the SyntaxTreeInterface/SynaxForestModel.
   */
  
  public final Operator getOperator( final IRNode n )
  {
    synchronized( structLock ) {
      return synForestModCore.getOperator(n);
    }
  }


  public final boolean opExists( final IRNode n )
  {
    synchronized( structLock ) {
      return synForestModCore.opExists(n);
    }
  }

  public final void initNode( final IRNode n, final Operator op )
  {
    synchronized( structLock ) {
      synForestModCore.initNode(n, op);
    }
  }

  public final void initNode( final IRNode n, final Operator op, final int min )
  {
    synchronized( structLock ) {
      synForestModCore.initNode(n, op, min);
    }
  }

  public final void initNode( final IRNode n, final Operator op, final IRNode[] children )
  {
    synchronized( structLock ) {
      synForestModCore.initNode(n, op, children);
    }
  }
}

 
