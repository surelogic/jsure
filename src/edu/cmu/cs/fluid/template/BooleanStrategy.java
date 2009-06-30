/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/BooleanStrategy.java,v 1.5 2005/05/25 20:44:01 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * Strategy that only accepts {@link java.lang.Boolean} objects.
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public class BooleanStrategy
extends JavaClassStrategy
{
  /** Flyweight */
  public static final FieldStrategy prototype = new BooleanStrategy();

  /** 
   * Create a new Boolean Strategy 
   */
  public BooleanStrategy()
  {
    super( Boolean.TRUE.getClass() );
  }
}
