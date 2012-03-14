/*
 * Created on Sep 9, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.io.PrintStream;

import com.surelogic.ast.IType;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Interface for types that are stored in the type slot.
 * Use <tt>instanceof</tt> to determine what class something is of.
 * Use the factory to get instances.  This interface should
 * not be implemented by any class unknown to the factory.
 * 
 * Must use isEqualTo() for comparison
 * equals() by type formals is used to facilitate capture of type formals
 * 
 * @author chance
 * @see IJavaPrimitiveType
 * @see IJavaReferenceType
 * @see IJavaVoidType
 * @see JavaTypeFactory
 */
public interface IJavaType extends IType {
  public String getName();
  
  /**
   * Produce a new type in which any type parameters are substituted
   * by the given substitution
   * @param s the substitution to use for type formals.
   *    If a formal is <em>not</em> substituted, it remains.
   * @return new type (that is identical if there was no change).
   */
  public IJavaType subst(IJavaTypeSubstitution s);
  
  /**
   * @return true if this type is a subtype of t2
   */
  public boolean isSubtype(ITypeEnvironment env, IJavaType t2);
  
  public boolean isAssignmentCompatible(ITypeEnvironment env, IJavaType t2, IRNode e2);  
  
  public IJavaType getSuperclass(ITypeEnvironment env);
  
  public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env);
  
  /**
   * @return true if it and all the types it depends on are valid
   */
  public boolean isValid();
  public void printStructure(PrintStream out, int indent);
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2);
}
