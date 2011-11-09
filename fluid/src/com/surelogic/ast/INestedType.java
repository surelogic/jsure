/*$Header: /cvs/fluid/fluid/src/com/surelogic/ast/INestedType.java,v 1.1 2006/09/21 17:59:49 chance Exp $*/
package com.surelogic.ast;

public interface INestedType extends IDeclaredType {
  /** Return the type this type is nested in.
   * This type, itself, may be a nested type.
   */
  IDeclaredType getOuterType();
}
