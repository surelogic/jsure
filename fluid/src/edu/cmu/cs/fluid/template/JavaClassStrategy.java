/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/JavaClassStrategy.java,v 1.6 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * Scalar strategy that accepts an object based on its class.
 *
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public class JavaClassStrategy
extends AbstractScalarStrategy
{
  /** The representation of the class accepted by this strategy. */
  private Class goodClass;

  /**
   * Create a new strategy that accepts instances of the
   * given class.
   */
  public JavaClassStrategy( final Class c )
  {
    goodClass = c;
  }

  /**
   * Get the representation of the class accepted by this strategy
   */
  public Class getAcceptedClass()
  {
    return goodClass;
  }

  /**
   * Returns <code>true</code> iff obj is an
   * instance of the class accepted by this stratgy.
   */
  @Override
  protected boolean acceptableScalar( final Object obj )
  {
    return goodClass.isInstance( obj );
  }
}
