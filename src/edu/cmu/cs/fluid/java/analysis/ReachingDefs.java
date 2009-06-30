/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ReachingDefs.java,v 1.15 2007/07/10 22:16:29 aarong Exp $ */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * Compound lattice used by {@link ReachingDefAnalysis}.
 */

public class ReachingDefs
extends ArrayLattice<IRNode>
{
  private static final int LOCAL_NOT_FOUND = -1;
  final private IRNode[] locals;

  public ReachingDefs( final IRNode[] lcls  )
  {
    super( new UnionLattice<IRNode>(), lcls.length );
    this.locals = lcls;
  }

  protected ReachingDefs( final IRNode[] lcls, final Lattice<IRNode>[] values,
                          final RecordLattice<IRNode> top, final RecordLattice<IRNode> bot )
  {
    super( values, top, bot );
    locals = lcls;
  }

  @Override
  protected RecordLattice<IRNode> newLattice( final Lattice<IRNode>[] newValues )
  {
    return new ReachingDefs( locals, newValues, top, bottom );
  }

  public UnionLattice<IRNode> getLatticeTop() {
    // this condition is only true when a warning
    // message is about to be printed:
    if (locals.length == 0) return new UnionLattice<IRNode>();
    // assume at least one def
    return (UnionLattice<IRNode>)getValue(0).top();
  }

  public UnionLattice<IRNode> getLatticeBottom() {
    // this condition is only true when a warning
    // message is about to be printed:
    if (locals.length == 0) return (UnionLattice<IRNode>)(new UnionLattice()).bottom();
    // assume at least one def
    return (UnionLattice<IRNode>)getValue(0).bottom();
  }

  public ReachingDefs replaceValue( final IRNode var, final IRNode def )
  {
    return replaceValue( var, (SetLattice<IRNode>)getLatticeTop().addElement( def ) );
  }

  public ReachingDefs replaceValue( final IRNode var, final SetLattice<IRNode> ul )
  {
    final int i = findLocal( var );
    if( i != LOCAL_NOT_FOUND ) {
      return (ReachingDefs)super.replaceValue( i, ul );
    } else {
      // for now
      throw new IllegalArgumentException ( "Couldn't find local: " + DebugUnparser.toString(var));
    }
  }

  private int findLocal( final IRNode local )
  {
    for( int i = 0; i < locals.length; i++ )
    {
      if( locals[i].equals( local ) ) {
        return i;
      }
    }
    return LOCAL_NOT_FOUND;
  }

  //-------------------------------------------------------------

  /** 
   * Return the set of reaching definitions for a given local
   * variable.
   * @return The set of reaching defs for the given variable.
   * The set contains IRNodes whose operator type is 
   * ParamaterDeclaration, VariableDeclarator or a subclass
   * of Assignment.  If it is ParameterDeclaration, then the
   * value of the use comes from the parameter passed to the
   * method.  If it is VariableDeclarator, it is the declaration
   * of the local variable;
   * in the latter, an assignment to the variable.  A 
   * VariableDeclarator may have NoInitialization as the 
   * variable's initialization, in which case it indicates that
   * the local may not have been initialized.
   * @exception IllegalArgumentException Thrown if the 
   * variable is not defined in the lattice.
   */
  public SetLattice getReachingDefsFor( final IRNode local )
  {
    final int i = findLocal( local );
    if( i != LOCAL_NOT_FOUND ) {
      return (SetLattice)getValue( i );
    }
    throw new IllegalArgumentException( "Local not found: " + DebugUnparser.toString(local));
  }

  //-------------------------------------------------------------

  /**
   * Returns <code>true</code> iff <code>node</code> is a 
   * VariableDeclarator with a NoInitialization node as
   * its initialization expression.
   */
  public static boolean isDefNoInit( final IRNode node )
  {
    final Operator op = JJNode.tree.getOperator( node );
    if( VariableDeclarator.prototype.includes( op ) )
    {
      final IRNode init = VariableDeclarator.getInit( node );
      final Operator initOp = JJNode.tree.getOperator( init );
      if( NoInitialization.prototype.includes( initOp ) ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns <code>true</code> if the given set contains
   * a variable declaration that is uninitialized.
   * Used to determine if a variable is possibly unitialized.
   */
  private static boolean containsNoInit( final SetLattice ul )
  {
    final Iterator iter = ul.iterator();
    while( iter.hasNext() )
    {
      final IRNode node = (IRNode)iter.next();
      if( isDefNoInit( node ) ) {
        return true;
      }
    }
    return false;
  }

  //-------------------------------------------------------------

  /**
   * Returns <code>true</code> iff the set is not empty and 
   * does not contain a varible declaration that is unitialized.
   * Used to determine if a variable is definately defined.
   */
  public static boolean isDefinitelyDefined( final SetLattice ul )
  {
    if( !ul.isEmpty() ) {
      return !containsNoInit( ul );
    } else {
      return false;
    }
  }

  /**
   * Returns <code>true</code> iff the set has more than 1 
   * element.  Can be used to determine if the a variable has
   * more than one reaching def.
   */
  public static boolean isReachingDefAmbiguous( final SetLattice ul )
  {
    return (ul.size() > 1);
  }

  /**
   * returns <code>true</code> iff the contains exactly one 
   * element, and that element is not a 
   * VariableDeclarator with a NoInitialization node as
   * its initialization expression.
   */
  public static boolean hasSingleReachingDef( final SetLattice ul )
  {
    return isDefinitelyDefined( ul ) && !isReachingDefAmbiguous( ul );
  }

  /**
   * returns <code>true</code> iff the set is empty or if the
   * the set contains a VariableDeclarator with a NoInitialization
   * node as its init expression.
   */
   public static boolean isUndefined( final SetLattice ul )
   {
     if( ul.isEmpty() ) {
       return true;
     } else if( ul.size() == 1 ) {
       return containsNoInit( ul );
     } else {
       return false;
     }
   }

  //-------------------------------------------------------------

  public boolean isDefinitelyDefined( final IRNode local )
  {
    final SetLattice fl = getReachingDefsFor( local );
    return isDefinitelyDefined( fl );
  }

  public boolean isReachingDefAmbiguous( final IRNode local )
  {
    final SetLattice fl = getReachingDefsFor( local );
    return isReachingDefAmbiguous( fl );
  }

  public boolean hasSingleReachingDef( final IRNode local )
  {
    final SetLattice fl = getReachingDefsFor( local );
    return hasSingleReachingDef( fl );
  }

  public boolean isUndefined( final IRNode local )
  {
    final SetLattice fl = getReachingDefsFor( local );
    return isUndefined( fl );
  }
}
