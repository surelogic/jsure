// $Header$
package com.surelogic.ast;

import edu.cmu.cs.fluid.sea.drops.promises.*;

public interface ILockBinding extends IPromiseBinding {
  //IAbstractLockDeclarationNode getNode();
  ModelDrop<?> getDrop();
}
