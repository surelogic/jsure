/*$Header: /cvs/fluid/fluid/src/com/surelogic/task/ITaskListener.java,v 1.3 2007/06/25 21:31:45 ethan Exp $*/
package com.surelogic.task;

/**
 * Used by the TaskManager to notify objects of the running state of the manager.
 * @author Ethan Urie
 */
public interface ITaskListener
{
	/**
	 * Method called when all tasks in the graph have been processed
	 */
	public void allTasksComplete();
	
	/**
	 * Method called when a task has been started
	 * @param task The task that has been started
	 */
	public void taskStarted(Runnable task);
	
	/**
	 * Method called when a task has been completed
	 * @param task The task that has been completed
	 */
	public void taskCompleted(Runnable task);
	
	/**
	 * Method called when the manager starts to process tasks
	 */
	public void startingTasks();
}
