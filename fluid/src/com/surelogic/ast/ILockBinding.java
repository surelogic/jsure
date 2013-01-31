// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.drops.ModelDrop;

public interface ILockBinding extends IPromiseBinding {
  //IAbstractLockDeclarationNode getNode();
  @Override
  ModelDrop<?> getDrop();
}
