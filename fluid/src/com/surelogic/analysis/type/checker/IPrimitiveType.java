package com.surelogic.analysis.type.checker;

public interface IPrimitiveType extends IType {
  // to do
  
  /**
   * Can the given type be widened to this type?  Allows identity conversion,
   * so, for example, <code>int</code> can be widened to <code>int</code>.
   */
  public boolean canWiden(IType type);
}
