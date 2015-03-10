// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/IWildcardType.java,v 1.2 2006/09/21 17:59:49 chance Exp $
package com.surelogic.ast;


public interface IWildcardType extends IDerivedRefType {
  public IReferenceType getUpperBound();
  public IReferenceType getLowerBound();
}
