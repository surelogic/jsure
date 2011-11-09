// $Header$
package com.surelogic.ast;

import edu.cmu.cs.fluid.sea.drops.promises.*;

public interface IRegionBinding extends IPromiseBinding {
//  IRegionDeclarationNode getNode();
  RegionModel getDrop();
}
