// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/ITypeFormal.java,v 1.2 2006/09/21 17:59:49 chance Exp $
package com.surelogic.ast;

import com.surelogic.ast.java.operator.ITypeFormalNode;

public interface ITypeFormal extends ISourceRefType {
  @Override
  public ITypeFormalNode getNode();
  public IReferenceType getExtendsBound();
}
