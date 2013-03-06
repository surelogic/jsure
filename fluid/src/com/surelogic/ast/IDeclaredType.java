// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/IDeclaredType.java,v 1.1 2006/09/01 14:26:59 chance Exp $
package com.surelogic.ast;

import com.surelogic.ast.java.operator.ITypeDeclarationNode;

public interface IDeclaredType extends ISourceRefType {
  @Override
  public ITypeDeclarationNode getNode();
}
