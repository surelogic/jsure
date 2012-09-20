package com.surelogic.annotation.rules;

import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.VouchPromiseDrop;

public final class VouchProcessorConsistencyProofHook extends AbstractSeaConsistencyProofHook {

  @Override
  public void preConsistencyProof(Sea sea) {
    for (final ResultDrop rd : sea.getDropsOfType(ResultDrop.class)) {
      if (!rd.isConsistent()) {
        if (rd.getNode() == null) {
          continue; // No possible vouch
        }
        VouchPromiseDrop vouch = VouchRules.getEnclosingVouch(rd.getNode());
        if (vouch != null) {
          rd.setVouched();
          rd.addTrusted(vouch);
          vouch.addSupportingInformation(rd.getNode(), "(analysis result vouched for) " + rd.getMessage());
        }
      }
    }
  }
}
