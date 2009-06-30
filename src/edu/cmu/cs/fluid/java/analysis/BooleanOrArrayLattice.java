/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/BooleanOrArrayLattice.java,v 1.3 2007/07/05 18:15:14 aarong Exp $ */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.util.ArrayLattice;
import edu.cmu.cs.fluid.util.BooleanLattice;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.RecordLattice;

/**
 * Compound lattice used by {@link StaleValueAnalysis}.
 * Lattice is a record of Boolean "OR" Lattices, one for each
 * local variable in the method being analyzed.  The initial value
 * is that everything is definately unassigned, although when the
 * DefinateUnassignmentAnalysis initializes itself it sets all the
 * method parameters to be NOT definately unassigned.  
 */

public class BooleanOrArrayLattice
extends ArrayLattice<Boolean>
{
  private static final int LOCAL_NOT_FOUND = -1;
  private static final BooleanLattice TRUE =
    (BooleanLattice) BooleanLattice.orLattice.bottom();
  private static final BooleanLattice FALSE = 
    (BooleanLattice) BooleanLattice.orLattice.top(); 
  final private IRNode[] locals;

  /**
   * Create a new lattice value with the given local variables.
   * All the nested lattices are initialzied to TOP (i.e., false).
   * @param lcls Array of VariableDeclarators corresponding to the
   * local variables of the method, ParameterDeclarations corresponding
   * to the method's parameters, possibly a ReceiverDeclaration if the
   * method has a receiver, and ReturnValueDeclaration corresponding to
   * the return value of the method.
   */
  public BooleanOrArrayLattice( final IRNode[] lcls  )
  {
    super( BooleanLattice.andLattice, lcls.length );
    this.locals = lcls;
  }

  /**
   * Create a new lattice value with the given local variables,
   * given lattice values, and given top and bottom values.
   * @param lcls Array of VariableDeclarators corresponding to the
   * local variables of the method, ParameterDeclarations corresponding
   * to the method's parameters, possibly a ReceiverDeclaration if the
   * method has a receiver, and ReturnValueDeclaration corresponding to
   * the return value of the method.
   * @param values Array of lattices values corresponding to the given
   * local variables
   * @param top The top value for the compound lattice
   * @param bot The bottom value for the compound lattice
   */
  protected BooleanOrArrayLattice(
    final IRNode[] lcls, final Lattice<Boolean>[] values,
    final RecordLattice<Boolean> top, final RecordLattice<Boolean> bot )
  {
    super( values, top, bot );
    locals = lcls;
  }

  /**
   * Factory method for creating a new lattice from the given array of
   * lattice values.
   */
  @Override
  protected RecordLattice<Boolean> newLattice( final Lattice<Boolean>[] newValues )
  {
    return new BooleanOrArrayLattice( locals, newValues, top, bottom );
  }

  /**
   * Create a new lattice by replacing the value for the given variable
   * with a new value.
   * @param var A VariableDeclarator, ParameterDeclaration, ReceiverDeclaration,
   * or ReturnValueDeclaration node.
   * @param val The new value
   * @return A new lattice value appropriately updated.
   */
  public BooleanOrArrayLattice replaceValue( final IRNode var, final boolean val )
  {
    return replaceValue( var, val ? TRUE : FALSE );
  }

  /**
   * Create a new lattice by replacing the value for the given variable
   * with a new value.
   * @param var A VariableDeclarator, ParameterDeclaration, ReceiverDeclaration,
   * or ReturnValueDeclaration node.
   * @param val The new lattice value.
   * @return A new lattice value appropriately updated.
   */
  public BooleanOrArrayLattice replaceValue(
    final IRNode var, final BooleanLattice val )
  {
    final int i = findLocal( var );
    if( i != LOCAL_NOT_FOUND ) {
      return (BooleanOrArrayLattice)super.replaceValue( i, val );
    } else {
      // for now
      System.err.println( "Couldn't find local: " +
			  DebugUnparser.toString(var));
      return this;
    }
  }

  /**
   * Get the position of the given variable declaration in the
   * compound lattice.
   * @param local A VariableDeclarator, ParameterDeclaration, ReceiverDeclaration,
   * or ReturnValueDeclaration node.
   * @return The position of the associated variable in the compound lattice
   * or {@link #LOCAL_NOT_FOUND} if the variable is not found in the lattice.
   */
  private int findLocal( final IRNode local )
  {
    for( int i = 0; i < locals.length; i++ ) {
      if( locals[i].equals( local ) ) {
        return i;
      }
    }
    return LOCAL_NOT_FOUND;
  }

  //-------------------------------------------------------------

  /**
   * Get the value for a particular local variable.
   * @exception IllegalArgumentException Thrown if the variable is not
   * part of the lattice.
   */
  public boolean getValue( final IRNode local )
  {
    final int i = findLocal( local );
    if( i != LOCAL_NOT_FOUND ) {
      return ((BooleanLattice) getValue( i )).getValue();
    } else {
      throw new IllegalArgumentException( "Local Not Found" );
    }
  }

  //-------------------------------------------------------------
}
