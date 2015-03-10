// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/ConstantAttributePredicate.java,v 1.7 2003/07/15 18:39:11 thallora Exp $

package edu.cmu.cs.fluid.mvc.predicate;


public class ConstantAttributePredicate
  extends AbstractAttributePredicate
{
  public static final ConstantAttributePredicate ALL =
    new ConstantAttributePredicate( true );
  
  public static final ConstantAttributePredicate NONE =
    new ConstantAttributePredicate( false );

  private final boolean value;

  private ConstantAttributePredicate( final boolean v )
  {
    value = v;
  }

  @Override
  public String getLabel()
  {
    return value ? "All" : "None";
  }

  @Override
  public boolean includesValue( final Object v )
  {
    return value;
  }
}
