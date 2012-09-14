/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.promises;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.PromiseDrop;


public interface IDerivedDropCreator<P extends PromiseDrop<? extends IAASTRootNode>>
extends ValidatedDropCallback<P> {
	// Just a marker
}
