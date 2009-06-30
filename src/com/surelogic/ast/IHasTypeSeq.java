/*$Header: /cvs/fluid/fluid/src/com/surelogic/ast/IHasTypeSeq.java,v 1.1 2007/06/12 21:26:32 chance Exp $*/
package com.surelogic.ast;

public interface IHasTypeSeq extends ResolvableToType {
  Iterable<? extends IType> resolveType();
}
