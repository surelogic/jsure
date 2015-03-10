/*
 * Created on Sep 9, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.IReferenceType;

/**
 * Interface for the various kinds of reference types something could be.
 * @author chance
 * @see IJavaArrayType
 * @see IJavaDeclaredType
 * @see IJavaNullType
 * @see IJavaIntersectionType
 */
public interface IJavaReferenceType extends IJavaType, IReferenceType {

}
