/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.modules;

import com.surelogic.aast.promise.ModuleChoiceNode;

public class ModuleScopePromiseDrop extends ModulePromiseDrop {

	ModuleScopePromiseDrop(ModuleChoiceNode mcn) {
		super(mcn, mcn.getModScope().getModuleName());
	}

}
