/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/ISourceRefType.java,v 1.3 2007/06/29 14:41:38 chance Exp $*/
package com.surelogic.aast.bind;

public interface ISourceRefType extends IReferenceType {
  boolean fieldExists(String id);
  IVariableBinding findField(String id);
}
