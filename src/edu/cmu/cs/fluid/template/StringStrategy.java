/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/StringStrategy.java,v 1.5 2005/05/25 20:44:01 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * Strategy that only accepts {@link java.lang.String} objects.
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public class StringStrategy
extends JavaClassStrategy
{
  /** Flyweight */
  public static final FieldStrategy prototype = new StringStrategy();

  /** 
   * Create a new String Strategy 
   */
  public StringStrategy()
  {
    super( "foo".getClass() );
  }
}
