/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaWildcardType.java,v 1.3 2007/04/27 07:23:00 boyland Exp $
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.IWildcardType;


/**
 * The type of a wildcard.  This type can only be used as a actual type parameter.
 * It will have either an upper bound, or a lower bound, but not both.
 * @author boyland
 */
public interface IJavaWildcardType extends IJavaReferenceType, IWildcardType {
  /** Get the upper bound (if any).
   * @return upper bound (or null, if none)
   * @see com.surelogic.ast.IWildcardType#getUpperBound()
   */
  public IJavaReferenceType getUpperBound();
  /** Get the lower bound (if any)
   * @return lower bound (or null, if none)
   * @see com.surelogic.ast.IWildcardType#getLowerBound()
   */
  public IJavaReferenceType getLowerBound();
}
