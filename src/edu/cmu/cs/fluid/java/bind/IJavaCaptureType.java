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
  IJavaWildcardType getWildcard();
  
  /** Get the upper bound (if any), e.g. ? super X
   * @return upper bound (or null, if none)
   * @see com.surelogic.ast.ICaptureType#getUpperBound()
   */
  public IJavaReferenceType getUpperBound();
  
  /** Get the greatest lower bound (if any), e.g. ? extends X, 
   * by combining the lower bounds from the wildcard type and the type parameter
   *  
   * @return lower bound (or null, if none)
   * @see com.surelogic.ast.ICaptureType#getLowerBound()
   */
  public IJavaReferenceType getLowerBound();
}
