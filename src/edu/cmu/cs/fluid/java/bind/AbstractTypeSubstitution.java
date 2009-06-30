/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractTypeSubstitution.java,v 1.1 2007/11/29 17:45:16 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;

public abstract class AbstractTypeSubstitution implements IJavaTypeSubstitution {
  protected final IBinder binder;
  
  AbstractTypeSubstitution(IBinder b) {
    binder = b;
  }
  
  /**
   * Capture the wildcard type, if any
   * Otherwise, returns the original type
   * 
   * @param decl The declaration for a type formal
   * @param jt   The corresponding actual type
   */
  protected final IJavaType captureWildcardType(IRNode decl, IJavaType jt) {
    if (jt instanceof IJavaWildcardType) {
      IJavaWildcardType wt = (IJavaWildcardType) jt;
      IRNode irBounds        = TypeFormal.getBounds(decl);
      List<IJavaReferenceType> bounds;
      if (JJNode.tree.hasChildren(irBounds)) {
        bounds = new ArrayList<IJavaReferenceType>();
        for(IRNode b : MoreBounds.getBoundIterator(irBounds)) {
          bounds.add((IJavaReferenceType) binder.getJavaType(b)); 
        }
      } else {
        bounds = Collections.emptyList();
      }
      return JavaTypeFactory.getCaptureType(wt, bounds);
    }
    return jt;
  }
  
  /**
   * Apply this substitution to all types in the input list
   * (which is not modified).  If there are no changes, the result
   * <em>may</em> be identical to the input.  The result should
   * be considered immutable.
   * @param types list of types to substitute
   * @return immutable list of substituted types.
   */
  public final List<IJavaType> substTypes(List<IJavaType> types) {
    if (types.isEmpty()) return types;
    boolean changed = false; // FIX unused?
    List<IJavaType> res = new ArrayList<IJavaType>();
    for (IJavaType jt : types) {
      IJavaType jtp = jt.subst(this);
      if (jtp != jt) changed = true;
      res.add(jtp);
    }
    if (!changed) return types;
    return res; //? new ImmutableList(res.toArray())
  }
}
