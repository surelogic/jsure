// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/IVoidType.java,v 1.1 2006/09/01 14:26:59 chance Exp $
package com.surelogic.ast;

import com.surelogic.ast.java.operator.IDeclarationNode;

public interface IVoidType extends IType {
  IVoidType VOID = new IVoidType() {
    @Override
    public String getName() {
      return "void";
    }

    @Override
    public boolean isAssignmentCompatibleTo(IType t) {
      return (t instanceof IVoidType);
    }

    /*
    public boolean isCastCompatibleTo(IType t) {
      return (t instanceof IVoidType);
    }
    */

    @Override
    public boolean isSubtypeOf(IType t) {
      return (t instanceof IVoidType);
    }

    @Override
    public IDeclarationNode getNode() {
      return null;
    }
  };
}
