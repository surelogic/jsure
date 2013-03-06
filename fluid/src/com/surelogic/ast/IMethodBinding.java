// $Header$
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;

public interface IMethodBinding extends IFunctionBinding {
  @Override
  IMethodDeclarationNode getNode();
}
