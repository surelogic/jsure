// $Header$
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;

public interface IConstructorBinding extends IFunctionBinding {
  @Override
  IConstructorDeclarationNode getNode();
}
