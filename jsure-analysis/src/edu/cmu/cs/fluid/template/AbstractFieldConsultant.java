/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/AbstractFieldConsultant.java,v 1.5 2005/05/25 20:44:01 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * A partial implementation of a field consultant.
 * The implementation of {@link #isObjectAcceptable( Field, Object[] )} 
 * returns true if every element of the array is acceptable based on
 * {@link #isObjectAcceptable( Field, int, Object )} given the appropriate
 * position.  
 */
@Deprecated
@SuppressWarnings("all")
public abstract class AbstractFieldConsultant
implements FieldConsultant
{
  public abstract boolean isObjectAcceptable( Field f, int pos, Object o );

  public boolean isObjectAcceptable( Field f, Object[] o )
  {
    for( int i = 0; i < o.length; i++ ) {
      if( !isObjectAcceptable( f, i, o[i] ) ) return false;
    }
    return true;
  }
}
