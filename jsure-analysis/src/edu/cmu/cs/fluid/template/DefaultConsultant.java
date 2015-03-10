/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/DefaultConsultant.java,v 1.4 2005/05/25 20:44:01 chance Exp $ */
package edu.cmu.cs.fluid.template;

/**
 * This class is the default <code>FieldConsultant</code> used by 
 * Fields if no consultant is provided to the constructor.  It
 * always returns <code>true</code>.
 */
@Deprecated
@SuppressWarnings("all")
public class DefaultConsultant
implements FieldConsultant
{
  /**
   * Always returns true.
   */
  public boolean isObjectAcceptable( Field f, int pos, Object o )
  {
    return true;
  }

  /**
   * Always returns true.
   */
  public boolean isObjectAcceptable( Field f, Object[] o )
  {
    return true;
  }
}
