// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.drops.regions.RegionModel;

public interface IRegionBinding extends IPromiseBinding {
//  IRegionDeclarationNode getNode();
  RegionModel getDrop();
}
