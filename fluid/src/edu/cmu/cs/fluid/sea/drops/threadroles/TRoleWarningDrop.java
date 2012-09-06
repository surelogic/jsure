package edu.cmu.cs.fluid.sea.drops.threadroles;

import edu.cmu.cs.fluid.sea.WarningDrop;

public class TRoleWarningDrop extends WarningDrop implements IThreadRoleDrop {
  // No changes from super, except to implement IThreadRoleDrop so that
  // regression tests can distinguish ThreadRole results from other results.
  public TRoleWarningDrop() {
    super();
  }
}
