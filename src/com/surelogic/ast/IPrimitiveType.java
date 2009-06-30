// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/IPrimitiveType.java,v 1.1 2006/09/01 14:26:59 chance Exp $
package com.surelogic.ast;

public interface IPrimitiveType extends IType {
  public enum Kind { 
    BOOLEAN(-1), BYTE(1), SHORT(3), CHAR(3), INT(5), LONG(7), FLOAT(9), DOUBLE(11);
    final String name;
    final int rank;
    Kind(int r) {
      rank = r;
      name = name().toLowerCase();
    }
    public String getName() {
      return name;
    }
    public int rank() { 
      return rank; 
    }
  }
  public Kind getKind();
}
