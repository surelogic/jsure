/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IHasLockBinding.java,v 1.1 2007/06/28 20:27:03 chance Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.ast.Resolvable;

public interface IHasLockBinding extends Resolvable {
  ILockBinding resolveBinding();
}
