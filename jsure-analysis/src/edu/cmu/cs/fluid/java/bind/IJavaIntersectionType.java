/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaIntersectionType.java,v 1.1 2007/04/27 07:20:58 boyland Exp $*/
package edu.cmu.cs.fluid.java.bind;

/**
 * A type that is a subtype of two different types:
 * one primary, the other secondary.
 * This type may appear in the extends bounds of a type formal.
 * @see IJavaTypeFormal
 * @author boyland
 */
public interface IJavaIntersectionType extends IJavaReferenceType, Iterable<IJavaType> {
  /**
   * Return the <em>primary</em> supertype of this intersection type.
   * This will be the same result as
   * {@link edu.cmu.cs.fluid.java.bind.IJavaType#getSuperclass(edu.cmu.cs.fluid.java.bind.ITypeEnvironment)}
   */
  public IJavaReferenceType getPrimarySupertype();
  
  /**
   * Return the <em>secondary</em> super type.
   * @return secondary super type of this intersection type.
   */
  public IJavaReferenceType getSecondarySupertype();
}
