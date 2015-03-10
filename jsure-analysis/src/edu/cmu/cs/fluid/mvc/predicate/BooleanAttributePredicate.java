// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/BooleanAttributePredicate.java,v 1.8 2003/07/15 18:39:11 thallora Exp $

package edu.cmu.cs.fluid.mvc.predicate;



/**
 * Attribute predicate for Boolean types.
 */
public class BooleanAttributePredicate
  extends AbstractAttributePredicate
{
  public static final BooleanAttributePredicate TRUE = 
                        new BooleanAttributePredicate( Boolean.TRUE );
  public static final BooleanAttributePredicate FALSE =
                        new BooleanAttributePredicate( Boolean.FALSE );

  private Boolean goodValue;


  public static BooleanAttributePredicate getPredicate( final Boolean b )
  {
    return getPredicate( b.booleanValue() );
  }

  public static BooleanAttributePredicate getPredicate( final boolean b )
  {
    return b ? TRUE : FALSE;
  }

  private BooleanAttributePredicate( final Boolean v )
  {
    goodValue = v;
  }

  @Override
  public String getLabel()
  {
    return goodValue.toString();
  }

  @Override
  public boolean includesValue( final Object value )
  {
    return goodValue.equals( value );
  }
}
