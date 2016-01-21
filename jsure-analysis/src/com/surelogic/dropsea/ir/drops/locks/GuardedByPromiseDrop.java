/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.locks;

import com.surelogic.aast.promise.GuardedByNode;
import com.surelogic.aast.promise.ItselfNode;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.IDerivedDropCreator;

import edu.cmu.cs.fluid.java.JavaGlobals;

public class GuardedByPromiseDrop extends PromiseDrop<GuardedByNode> implements IDerivedDropCreator<PromiseDrop<?>> {

  public GuardedByPromiseDrop(GuardedByNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }

  @Override
  public void validated(final PromiseDrop<?> lm) {
    lm.setVirtual(true);
    lm.setSourceDrop(this);
  }
  
  public boolean itself() {
    return getAAST().getLock() instanceof ItselfNode;
  }
}
