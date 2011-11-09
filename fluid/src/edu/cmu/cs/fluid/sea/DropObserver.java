package edu.cmu.cs.fluid.sea;

public interface DropObserver {

	/**
	 * Notification that something about the knowledge status of a drop has
	 * changed.
	 * 
	 * @param drop
	 *            the drop the notification is about.
	 * @param event
	 *            what happened to the drop.
	 */
	void dropChanged(Drop drop, DropEvent event);
}
