// $Header: /cvs/fluid/fluid/src/com/surelogic/ast/ICaptureType.java,v 1.2 2006/09/21 17:59:49 chance Exp $
package com.surelogic.ast;

public interface ICaptureType extends IDerivedRefType {
  public IWildcardType getWildcard();
  public IReferenceType getUpperBound();
  public IReferenceType getLowerBound();
}
