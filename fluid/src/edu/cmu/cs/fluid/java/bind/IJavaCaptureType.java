/*
 * Created on May 26, 2004
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.ICaptureType;

/**
 * "capture" type derived from a wildcard type
 * @author chance
 */
public interface IJavaCaptureType extends IJavaReferenceType, ICaptureType {
  @Override
  IJavaWildcardType getWildcard();
  
  /** Get the lower bound (if any), e.g. ? super X.
   * (See JLS 7 page 63)
   * 
   * @return lower bound (or null, if none)
   * @see com.surelogic.ast.ICaptureType#getLowerBound()
   */
  @Override
  public IJavaReferenceType getLowerBound();
  
  /** Get the least upper bound (if any), e.g. ? extends X, 
   * by combining the upper bounds from the wildcard type and the type parameter.
   * (See JLS 7 page 63)
   *  
   * @return upper bound (or null, if none)
   * @see com.surelogic.ast.ICaptureType#getUpperBound()
   */
  @Override
  public IJavaReferenceType getUpperBound();
}
