/*
 * Created on May 26, 2004
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.List;

import com.surelogic.ast.ICaptureType;

/**
 * "capture" type derived from a wildcard type
 * @author chance
 */
public interface IJavaCaptureType extends IJavaReferenceType, ICaptureType {
  IJavaWildcardType getWildcard();
  
  /** In Java 1.5 and later, return the type parameters
   * as an immutable list of
   * {@link edu.cmu.cs.fluid.java.bind.IJavaType} objects.
   */
  List<IJavaReferenceType> getTypeBounds();
}
