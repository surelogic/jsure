package edu.cmu.cs.fluid.sea;

public interface SeaObserver {
	/**
	 * This method is called when a project is closed or the JSure focus project
	 * changes. It is also called when a clean and build is done on the JSure
	 * focus project. To guard against doing something when there is still an
	 * open JSure focus project that is being cleaned and rebuilt use the below
	 * snippet of code:
	 * 
	 * <pre>
	 * if (!Nature.hasNatureAnyProject()) {
	 * }
	 * </pre>
	 * <p>
	 * It is <i>not</i> called any time the sea changes.
	 */
	void seaChanged();
}
