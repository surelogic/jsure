/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/ValidatedDropCallback.java,v 1.2 2007/07/20 16:46:12 chance Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

public interface ValidatedDropCallback<P extends PromiseDrop<? extends IAASTRootNode>> {
  void validated(P pd);
}
