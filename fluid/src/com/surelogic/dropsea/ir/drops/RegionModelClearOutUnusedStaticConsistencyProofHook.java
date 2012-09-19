package com.surelogic.dropsea.ir.drops;

import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.Sea;

public class RegionModelClearOutUnusedStaticConsistencyProofHook extends AbstractSeaConsistencyProofHook {

  @Override
  public void preConsistencyProof(Sea sea) {
    // Clear out unused Static regions
    for (RegionModel rm : sea.getDropsOfType(RegionModel.class)) {
      if (RegionRules.STATIC.equals(rm.getName())) {
        /*
         * If a "Static" region on a class has no deponents and one dependent to
         * java.lang.Object.All
         */
        if (!rm.hasDeponents() && rm.getDependentCount() == 1) {
          RegionModel.removeFromMapOfKnownRegions(rm);
          rm.invalidate();
        }
      }
    }
  }
}
