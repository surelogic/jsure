/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IRegionBinding.java,v 1.3 2007/07/16 19:22:02 chance Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.drops.RegionModel;


public interface IRegionBinding {
  /**
   * @return The corresponding RegionModel (drop)
   */
  RegionModel getModel();

  /**
   * @return the corresponding IRegion (possibly the same as getModel())
   */
  IRegion getRegion();
}
