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
  /** Get the lower bound (if any), e.g. ? super X .
   * 
   * (See JLS 7 page 63)
   * @return lower bound (or null, if none)
   * @see com.surelogic.ast.IWildcardType#getLowerBound()
   */
  @Override
  public IJavaReferenceType getLowerBound();
  
  /** Get the upper bound (if any), e.g. ? extends X 
   * 
   * (See JLS 7 page 63)
   * @return upper bound (or null, if none)
   * @see com.surelogic.ast.IWildcardType#getUpperBound()
   */
  @Override
  public IJavaReferenceType getUpperBound();
}
