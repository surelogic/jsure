package com.surelogic.dropsea.ir.drops;

import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.Sea;

public class CUDropClearOutAfterAnalysisProofHook extends AbstractSeaConsistencyProofHook {

  @Override
  public void postConsistencyProof(Sea sea) {
    // Clear out no longer needed CU drops before we snapshot the results.
    for (CUDrop cuDrop : sea.getDropsOfType(CUDrop.class)) {
      cuDrop.invalidate();
    }
    // Clear out proposed promises with null Java references.
    for (ProposedPromiseDrop pp : sea.getDropsOfType(ProposedPromiseDrop.class)) {
      if (pp.getJavaRef() == null || pp.getAssumptionRef() == null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(293, pp.getMessage()));
        pp.invalidate();
      }
    }
  }
}
