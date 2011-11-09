// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/IArrayType.java,v 1.1 2006/09/01 14:26:59 chance Exp $
package com.surelogic.ast;


public interface IArrayType extends IDerivedRefType {
  public IType getBaseType();
  public int getDimensions();
  public IType getElementType();
}
