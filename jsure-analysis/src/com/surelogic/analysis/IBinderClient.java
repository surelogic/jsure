package com.surelogic.analysis;

import edu.cmu.cs.fluid.java.bind.IBinder;

public interface IBinderClient {
	public IBinder getBinder();
	public void clearCaches();
}
