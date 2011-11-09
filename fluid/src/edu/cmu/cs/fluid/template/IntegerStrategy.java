/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/IntegerStrategy.java,v 1.5 2005/05/25 20:44:01 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * Strategy that only accepts {@link java.lang.Integer} objects.
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public class IntegerStrategy
extends JavaClassStrategy
{
  /** Flyweight */
  public static final FieldStrategy prototype = new IntegerStrategy();

  /** 
   * Create a new Integer Strategy 
   */
  public IntegerStrategy()
  {
    super( new Integer( 1 ).getClass() );
  }
}
