package edu.cmu.cs.fluid.sea;

public interface SeaObserver {
	/**
	 * This method is called when a project is closed or the JSure focus project
	 * changes. It is <i>not</i> called any time the sea changes.
	 */
	void seaChanged();
}
