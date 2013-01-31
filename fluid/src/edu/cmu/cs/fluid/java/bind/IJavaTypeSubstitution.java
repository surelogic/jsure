/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaTypeSubstitution.java,v 1.2 2008/07/02 15:45:13 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

/**
 * An interface for substituting type parameters
 * from both types and methods/constructors
 *
 * @author Edwin.Chan
 */
public interface IJavaTypeSubstitution {
  /**
   * Search for the substitution corresponding to the given type formal
   * (if any)
   */
  IJavaType get(IJavaTypeFormal jtf);

  ITypeEnvironment getTypeEnv();
  
  /**
   * Apply this substitution to all types in the input list
   * (which is not modified).  If there are no changes, the result
   * <em>may</em> be identical to the input.  The result should
   * be considered immutable.
   * @param types list of types to substitute
   * @return immutable list of substituted types.
   */
  List<IJavaType> substTypes(IJavaDeclaredType context, List<IJavaType> types);

  IJavaTypeSubstitution combine(IJavaTypeSubstitution other);
  
  static final IJavaTypeSubstitution NULL = new IJavaTypeSubstitution() {
    @Override
    public IJavaType get(IJavaTypeFormal jtf) {
      return jtf;
    }
    @Override
    public List<IJavaType> substTypes(IJavaDeclaredType context, List<IJavaType> types) {
      return types;
    }
    @Override
    public String toString() {
    	return "NULL SUBST";
    }

	@Override
  public IJavaTypeSubstitution combine(IJavaTypeSubstitution other) {
		return (other == null) ? this : other;
	}

	@Override
  public ITypeEnvironment getTypeEnv() {
		return null;
	}
  };
}
