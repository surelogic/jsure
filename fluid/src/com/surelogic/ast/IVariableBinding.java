// $Header$
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;
//import com.surelogic.ast.java.promise.*;

public interface IVariableBinding extends IBinding {
  IDeclaredType getContextType();
  IType convertType(IType t);
  @Override
  IVariableDeclarationNode getNode();
}
