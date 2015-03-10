package com.surelogic.dropsea.ir.drops;

import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.AnalysisResultDrop;
import com.surelogic.dropsea.ir.DropPredicate;
import com.surelogic.dropsea.ir.Sea;

public class ClearOutUnconnectedResultsProofHook extends AbstractSeaConsistencyProofHook {

  @Override
  public void preConsistencyProof(Sea sea) {
    sea.invalidateMatching(new DropPredicate() {
      @Override
      public boolean match(IDrop d) {
        if (d instanceof AnalysisResultDrop)
          return !((AnalysisResultDrop) d).checksAPromise();
        return false;
      }
    });
  }
}
