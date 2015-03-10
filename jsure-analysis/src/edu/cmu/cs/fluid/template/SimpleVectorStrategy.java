/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/SimpleVectorStrategy.java,v 1.6 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * A vector strategy that uses the same strategy for all its
 * positions.
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public class SimpleVectorStrategy
extends AbstractVectorStrategy
{
  /** The strategy that used for the vector's elements */
  private FieldStrategy strategy;

  /** 
   * Create a new vector strategy with the given minimum
   * and maximum sizes, and given strategy for the elements.
   */
  public SimpleVectorStrategy( final int min, final int max, final FieldStrategy s )
  {
    super( min, max );
    strategy = s;
  }
  
  /** 
   * Create a new vector strategy with a minimum size of 0,
   * and the given maximum size and strategy for the elements.
   */
  public SimpleVectorStrategy( final int max, final FieldStrategy s )
  {
    this( 0, max, s );
  }
  
  /** 
   * Create a new vector strategy with a minimum size of 0,
   * no maximum size, and the given strategy for the elements.
   */
  public SimpleVectorStrategy( final FieldStrategy s )
  {
    this( NO_MAXIMUM_SIZE, s );
  }
  
  // Inherit JavaDoc
  @Override
  protected boolean acceptableInPos( int pos, Object obj )
  {
    if( strategy.isScalar() ) {
      return strategy.acceptableObject( 0, obj );
    } else {
      try {
        return strategy.acceptableObject( (Object [])obj );
      } catch( final ClassCastException e ) {
        return false;
      }
    }
  }

  /**
   * Returns <code>true</code> indicating that the same strategy object
   * is used for each element of the vector.
   */
  @Override
  public boolean sameForAllPositions()
  {
    return true;
  }

  // Inherit JavaDoc
  @Override
  protected FieldStrategy getStrategyForPosImpl( int pos )
  {
    return strategy;
  }
}
