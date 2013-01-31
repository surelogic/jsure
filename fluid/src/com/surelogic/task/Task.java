package com.surelogic.task;

import java.util.*;

/**
 * A concrete implementation of the ITask interface that manages some of the
 * dependencies/dependents
 * 
 * @author ethan
 * @Lock is countLock protects dependencyCount
 * @Lock is markLock protects marked
 * @Lock is this protects Instance
 */
class Task implements ITask {
	// A list of tasks that this task depends on
	private final List<ITask> dependencies = new ArrayList<ITask>();

	// A list of dependent tasks
	private final List<ITask> dependents = new ArrayList<ITask>();

	// This task's payload and the thing that is actually going to be executed
	private final Runnable runnable;

	// This tasks name, should match the name in the taskNameTable in
	// TaskManager
	private final String name;

	// Flag indicating whether or not this task has been executed. This is set
	// when this
	// task is first passed to ThreadPoorExecutor.execute(Runnable)
	private TaskState state = TaskState.WAITING;

	// Used when finding cycles
	private boolean marked = false;
	private final Object markLock = new Object();

	// Counts the number of dependencies that haven't been met
	private int dependencyCount;

	// The TaskManager that is going to run this Task
	private final TaskManager manager;

	public Task(final String name, final Runnable runnable,
		final TaskManager manager) {
		dependencyCount = 0;
		this.runnable = runnable;
		this.name = name;
		this.manager = manager;
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized Iterator<ITask> getDependencies() {
		return dependencies.iterator();
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized Iterator<ITask> getDependents() {
		return dependents.iterator();
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void addDependency(ITask dependency) {
		if (!dependencies.contains(dependency)) {
			dependencies.add(dependency);
			dependency.addDependent(this);
			dependencyCount++;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public void addDependencies(ITask[] deps) {
		for (ITask task : deps) {
			addDependency(task);
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void removeDependency(ITask dependency) {
		if (dependencies.contains(dependency)) {
			dependencies.remove(dependency);
			dependency.removeDependent(dependency);
			dependencyCount--;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void removeAllDependencies() {
		for (ITask task : dependencies) {
			task.removeDependent(this);
		}
		dependencies.clear();
		dependencyCount = 0;
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void addDependent(ITask dependent) {
		if (!dependents.contains(dependent)) {
			dependents.add(dependent);
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void removeDependent(final ITask dependent) {
		if (dependents.contains(dependent)) {
			dependents.remove(dependent);
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void removeAllDependents() {
		dependents.clear();
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void dependencyMet() {
		if (dependencyCount > 0) {
			dependencyCount--;
		}

		if (dependencyCount == 0 && state == TaskState.WAITING) {

			manager.executeNow(this);
			state = TaskState.STARTED;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void notifyDependents() {
		for (ITask task : dependents) {
			task.dependencyMet();
		}
	}

	/**
	 * Catches and re-throws any Error. It notifies its dependents if that is the case, however, so any
	 * dependents may exhibit weird behavior if a dependency fails and throws an error. The Error is re-thrown
	 * so that the TaskUncaughtExceptionHandler can make sure afterExecute() is run and the latch is decremented
	 * so to avoid hanging the system.
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public void run() {
		try {
			runnable.run();
			synchronized (this) {
				state = TaskState.COMPLETE;
			}
		}
		catch (Error e) {
			notifyDependents();
			throw e;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized int getDependencyCount() {
		return dependencyCount;
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public synchronized void resetDependencyCount() {
		dependencyCount = dependencies.size();
	}

	/**
	 * Return the name of this Task.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public void markTask() {
		synchronized (markLock) {
			marked = true;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public void unmarkTask() {
		synchronized (markLock) {
			marked = false;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 */
	@Override
  public boolean isMarked() {
		synchronized (markLock) {
			return marked;
		}
	}

	/**
	 * @see com.surelogic.task.ITask
	 * @param executed
	 *            True if this task has, or is going to, be executed
	 */
	@Override
  public synchronized void resetState() {
		this.state = TaskState.WAITING;
	}

	/**
	 * @see com.surelogic.task.ITask
	 * @return True if this task has been handed to
	 *         ThreadPoolExecutor.execute(Runnable)
	 */
	@Override
  public synchronized TaskState state() {
		return state;
	}

	public static enum TaskState {
		WAITING, STARTED, COMPLETE
	}
}
