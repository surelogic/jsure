// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/EnumeratedAttributePredicate.java,v 1.8 2003/07/15 18:39:11 thallora Exp $

package edu.cmu.cs.fluid.mvc.predicate;

import edu.cmu.cs.fluid.ir.IREnumeratedType;

/**
 * Attribute predicate for enumerated types.  Selects based on a 
 * specific value in the enumeration.
 */
public class EnumeratedAttributePredicate
  extends AbstractAttributePredicate
{
  private IREnumeratedType.Element goodElement;

  public EnumeratedAttributePredicate( final IREnumeratedType.Element v )
  {
    goodElement = v;
  }

  @Override
  public String getLabel()
  {
    return goodElement.toString();
  }

  @Override
  public boolean includesValue( final Object value )
  {
    return goodElement.equals( value );
  }
}
