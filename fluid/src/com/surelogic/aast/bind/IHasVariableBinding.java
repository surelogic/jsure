/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IHasVariableBinding.java,v 1.1 2007/06/28 16:49:04 chance Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.ast.Resolvable;

public interface IHasVariableBinding extends Resolvable {
  IVariableBinding resolveBinding();
}
