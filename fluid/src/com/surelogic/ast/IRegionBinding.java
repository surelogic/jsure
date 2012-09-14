// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.drops.promises.RegionModel;

import edu.cmu.cs.fluid.sea.drops.promises.*;

public interface IRegionBinding extends IPromiseBinding {
//  IRegionDeclarationNode getNode();
  RegionModel getDrop();
}
