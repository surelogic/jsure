// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.drops.promises.ModelDrop;

public interface ILockBinding extends IPromiseBinding {
  //IAbstractLockDeclarationNode getNode();
  ModelDrop<?> getDrop();
}
