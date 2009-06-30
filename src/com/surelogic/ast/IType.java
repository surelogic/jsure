// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/IType.java,v 1.3 2007/07/10 22:16:34 aarong Exp $
package com.surelogic.ast;

public interface IType extends IBinding {
  public String getName();

  /**
   * Returns true if t is a subtype of this type,
   * according to JLS 3.0, section 4.10
   */
  public boolean isSubtypeOf(IType t);

  /**
   * Returns true if t is cast compatible to this type,
   * according to JLS 3.0, section 5.5
   */
  //public boolean isCastCompatibleTo(IType t);

  /**
   * Returns true if t is assignment compatible to this type,
   * according to JLS 3.0, section 5.2
   */
  public boolean isAssignmentCompatibleTo(IType t);
}
