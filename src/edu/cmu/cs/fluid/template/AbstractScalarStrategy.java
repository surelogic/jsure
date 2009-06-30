/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/AbstractScalarStrategy.java,v 1.6 2005/05/25 20:44:01 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * Abstract class for implementing scalar strategies.
 * A Subclass simply needs to implement the method
 * {@link #acceptableScalar}.
 *
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public abstract class AbstractScalarStrategy
implements FieldStrategy
{
  /**
   * The field must be scalar, so always returns <code>true</code>.
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * The field must be scalar, so always returns <code>false</code>.
   */
  public boolean isVector()
  {
    return false;
  }

  // Inherit JavaDoc
  public boolean acceptableObject( final Object[] objs )
  {
    if( objs.length == 1 ) {
      return acceptableScalar( objs[0] );
    } else {
      return false;
    }
  }

  // Inherit JavaDoc
  public boolean acceptableObject( final int pos, final Object obj )
  {
    if( pos == 0 ) {
      return acceptableScalar( obj );
    } else {
      return false;
    }
  }

  /**
   * Used to implement <code>acceptableObject</code> methods.
   * Should return <code>true</code> if the given object
   * can be stored in position 0 of the value.
   */
  protected abstract boolean acceptableScalar( Object obj );

  /**
   * The field must be scalar, so always returns <code>1</code>.
   */
  public int getMinSize()
  {
    return 1;
  }

  /**
   * The field must be scalar, so always returns <code>1</code>.
   */
  public int getMaxSize()
  {
    return 1;
  }

  /**
   * The field must be scalar, so always returns <code>true</code>.
   */
  public boolean sameForAllPositions()
  {
    return true;
  }

  // Inherit JavaDoc
  public FieldStrategy getStrategyForPos( final int pos )
  {
    if( pos == 0 ) {
      return null;
    } else {
      throw new ArrayIndexOutOfBoundsException( "Scalar strategy does not have position " + pos );
    }
  }
}
