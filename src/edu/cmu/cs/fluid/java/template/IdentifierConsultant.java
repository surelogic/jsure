/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/IdentifierConsultant.java,v 1.7 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.template;

import edu.cmu.cs.fluid.java.JavaPredicates;
import edu.cmu.cs.fluid.template.*;

/**
 * A field consultant that only allows <tt>String</tt>s representing
 * legal Java identifiers to be placed in the field.
 */
@Deprecated
@SuppressWarnings("all")
public class IdentifierConsultant
extends AbstractFieldConsultant
{
  public static IdentifierConsultant prototype = new IdentifierConsultant();

  private IdentifierConsultant() 
  {
    super();
  }

  @Override
  public boolean isObjectAcceptable( Field f, int pos, Object o )
  {    
    if( o instanceof String ) {
      final String s = (String)o;
      return JavaPredicates.isIdentifier( s );
    }
    return false;
  }
}
