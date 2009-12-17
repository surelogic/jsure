/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import edu.cmu.cs.fluid.java.bind.IBinder;

public interface IBinderClient {
	IBinder getBinder();
	void clearCaches();
}
