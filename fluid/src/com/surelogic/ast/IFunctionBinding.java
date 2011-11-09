// $Header$
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;

public interface IFunctionBinding extends IBinding {
  IDeclaredType getContextType();
  IType convertType(IType t);
  ISomeFunctionDeclarationNode getNode();
}
