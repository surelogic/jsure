// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.drops.promises.RegionModel;

public interface IRegionBinding extends IPromiseBinding {
//  IRegionDeclarationNode getNode();
  RegionModel getDrop();
}
