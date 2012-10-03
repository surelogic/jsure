package com.surelogic.dropsea.ir.drops;

import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.Sea;

public class CUDropClearOutAfterAnalysisProofHook extends AbstractSeaConsistencyProofHook {

  @Override
  public void postConsistencyProof(Sea sea) {
    // Clear out no longer needed CU drops before we snapshot the results.
    for (CUDrop cuDrop : sea.getDropsOfType(CUDrop.class)) {
      cuDrop.invalidate();
    }
  }
}
