/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.GuardedByNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class GuardedByPromiseDrop extends PromiseDrop<GuardedByNode> implements IDerivedDropCreator<LockModel> {

  public GuardedByPromiseDrop(GuardedByNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setMessage(getAAST().toString());
  }

  public void validated(final LockModel lm) {
    lm.setVirtual(true);
    lm.setSourceDrop(this);
  }
}
