// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.drops.RegionModel;

public interface IRegionBinding extends IPromiseBinding {
//  IRegionDeclarationNode getNode();
  @Override
  RegionModel getDrop();
}
