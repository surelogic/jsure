package edu.cmu.cs.fluid.sea.drops.threadroles;

import edu.cmu.cs.fluid.sea.InfoDrop;

public class TRoleInfoDrop extends InfoDrop implements IThreadRoleDrop {

	// No changes from super, except to implement IThreadRoleDrop so that
	// regression tests can distinguish ThreadRole results from other results.
	public TRoleInfoDrop() {
		super();
	}
}
