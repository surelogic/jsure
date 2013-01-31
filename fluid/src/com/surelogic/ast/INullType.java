// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/INullType.java,v 1.1 2006/09/01 14:26:59 chance Exp $
package com.surelogic.ast;

import com.surelogic.ast.java.operator.IDeclarationNode;

public interface INullType extends IReferenceType {
  INullType NULL = new INullType() {
    @Override
    public String getName() {
      return "null";
    }

    @Override
    public boolean isAssignmentCompatibleTo(IType t) {
      return t instanceof IReferenceType;
    }

    /*
    public boolean isCastCompatibleTo(IType t) {
      return t instanceof IReferenceType;
    }
    */

    @Override
    public boolean isSubtypeOf(IType t) {
      return t instanceof INullType;
    }

    @Override
    public IDeclarationNode getNode() {
      return null;
    }
  };
}
