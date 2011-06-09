/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.threadroles;

import edu.cmu.cs.fluid.sea.WarningDrop;

public class TRoleWarningDrop extends WarningDrop implements IThreadRoleDrop {
	// No changes from super, except to implement IThreadRoleDrop so that
	// regression tests can distinguish ThreadRole results from other results.
	public TRoleWarningDrop(String t) {
		super(t);
	}
}
