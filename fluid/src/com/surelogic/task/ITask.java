package com.surelogic.task;

import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;

import com.surelogic.task.Task.TaskState;

/**
 * Represents a generic task that can be run in its own thread. It becomes one node in a dependency graph
 * that is traversed by the TaskManager.
 * 
 * @author Ethan.Urie
 */
interface ITask extends Runnable {
	/**
	 * Returns the list of ITask objects that this task depends on.
	 * @return
	 */
	public Iterator<ITask> getDependencies();

	/**
	 * Returns the list of ITask objects that depend on this task.
	 * @return
	 */
	public Iterator<ITask> getDependents();


	/**
	 * Adds a single ITask as a dependency for this task and adds itself as a dependent of the dependency task.
	 * @param dependency
	 */
	public void addDependency(ITask dependency);

	/**
	 * Adds an array of ITask objects as dependencies. 
	 * @param deps
	 */
	public void addDependencies(ITask[] deps);

	/**
	 * Removes a single ITask object from the list of dependencies. Should notify the dependency that this task is no longer a dependent.
	 * @param dependency
	 */
	public void removeDependency(ITask dependency);
	
	/**
	 * Removes all of this task's dependencies and notifies them that this task is no longer a dependent.
	 *
	 */
	public void removeAllDependencies();

	/**
	 * Adds a single ITask object as a dependent of this task
	 * @param dependent
	 */
	public void addDependent(ITask dependent);

	/**
	 * Removes a single ITask from the list of dependents.
	 * @param dependent
	 */
	public void removeDependent(ITask dependent);

	/**
	 * Clears the list of dependents.
	 */
	public void removeAllDependents();
	
	/**
	 * Called when a task that this task depends on has completed.
	 */
	public void dependencyMet();

	/**
	 * Used to notify dependents that this task is complete.
	 */
	public void notifyDependents();
	
	
	/**
	 * Returns the number of dependencies that have not been met
	 * @return The number of dependencies that have not been met
	 */
	public int getDependencyCount();
	
	/**
	 * Resets the dependency count to the number of dependencies in the Vector
	 * 
	 */
	public void resetDependencyCount();
	
	/**
	 * Marks this task as seen before. Used to detect cycles.
	 */
	public void markTask();
	
	/**
	 * Removes the mark on this task.
	 */
	public void unmarkTask();
	
	/**
	 * Returns true if this task has been marked in the check for cycles.
	 * @return true if this task was marked
	 */
	public boolean isMarked();
	
	/**
	 * Sets an internal variable indicating that this Task has not been run
	 * from TaskManager.
	 */
	public void resetState();
	
	/**
	 * Returns whether or not this task has been passed to {@link ThreadPoolExecutor}.execute(Runnable)
	 * @return true if this task has been executed
	 */
	public TaskState state();
}
