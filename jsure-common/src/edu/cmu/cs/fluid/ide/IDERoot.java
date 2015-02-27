package edu.cmu.cs.fluid.ide;

import java.net.URL;

public abstract class IDERoot {
	protected IDERoot() {
		// Nothing to do
	}

	private static IDERoot instance;
	
	public static synchronized IDERoot getInstance() {
		return instance;
	}

	protected static synchronized void initInstance(IDERoot i) {
		if (/*instance != null ||*/ i == null) {
			throw new IllegalArgumentException();
		}
		instance = i;
	}
	
	public abstract URL getResourceRoot();

	public abstract String getStringPreference(String pref);
}
