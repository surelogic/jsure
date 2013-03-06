/*
 * $Header: /cvs/fluid/fluid/src/com/surelogic/task/TaskManager.java,v 1.13
 * 2007/06/29 13:10:37 ethan Exp $
 */
package com.surelogic.task;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.*;

/**
 * This is a class designed to manage and run a set of Runnable tasks that may
 * have multiple interdependencies. It subclasses
 * {@link java.util.concurrent.ThreadPoolExecutor} and uses its capabilities
 * combined with an implementation of our {@link com.surelogic.task.ITask}
 * interface, {@link com.surelogic.task.Task}.
 * 
 * It also provides notification means, via the
 * {@link com.surelogic.task.ITaskListener} interface.
 * 
 * Unlike {@link java.util.concurrent.ThreadPoolExecutor}, the
 * {@link #execute(Runnable)} method should never be called by any client, it
 * will throw an {@link UnsupportedOperationException}. Instead, users of this
 * class should call {@link #addTask(String, Runnable)} to add a Runnable task
 * to the graph, set its dependencies via
 * {@link #addDependencies(String, String[])} or
 * {@link #addDependency(String, String)} and then run the entire graph by
 * calling {@link #execute(boolean)}. If a boolean value of <code>true</code>
 * is sent to this argument, the method will not return before all tasks are
 * complete.
 * 
 * 
 * @author Ethan.Urie
 * @Lock is listenerLock protects listeners
 * @Lock is taskLock protects taskLockNames
 * @Lock is stateLock protects isProcessing
 */
public class TaskManager extends ThreadPoolExecutor {
	/**
	 * The collection of ITask objects that must be run in order of their
	 * dependencies. This vector contains only those tasks that have no
	 * dependencies.
	 */
	private final List<ITask> headTasks = new ArrayList<ITask>();

	// Default amount of time for all tasks to complete
	private final static long AWAIT_TIME = 180;

	// This is a map of ITask objects to their unique names
	private final ConcurrentHashMap<String, ITask> taskNameTable =
		new ConcurrentHashMap<String, ITask>();

	// Used to keep track of which task names have placeholders -
	// NOTE: this should only be modified from the add/removePlaceHolder methods
	private final List<String> placeholders = new ArrayList<String>();

	// A task name used by an ITask that holds the place of a
	private final static String PLACEHOLDER = "PLACEHOLDER";

	// Managers the ITaskListeners
	private final TaskEventManager eventManager = new TaskEventManager();

	/**
	 * The collection of ITaskListeners that wish to be notified of the state
	 * changes of this manager.
	 */
	private final CopyOnWriteArrayList<ITaskListener> listeners =
		new CopyOnWriteArrayList<ITaskListener>();

	// Protects the listeners collection
	private final Object listenerLock = new Object();

	// Protects the task graph and taskNameTable
	private final Object taskLock = new Object();

	// Used by execute(boolean) to wait for all tasks to be executed before it
	// returns
	private CountDownLatch latch = null;

	// Used to ensure that other threads don't try to modify the graph after the
	// TaskManager has started to run.
	private int processing = 0;
	// Used to protect accesses to processing
	private final Object stateLock = new Object();
	
	private final boolean testing;

	/**
	 * Constructor that creates a TaskManager with a core pool size of N+1
	 * threads and a maximum pool size of (N*2)+1 threads where N = Number of
	 * processors. The keepAlive time is 60 seconds.
	 * 
	 * @param workQueue
	 */
	public TaskManager(BlockingQueue<Runnable> workQueue) {
		super(Runtime.getRuntime().availableProcessors() + 1, (Runtime
			.getRuntime().availableProcessors() * 2) + 1, 60, TimeUnit.SECONDS,
			workQueue);
		testing = false;
	}

	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 */
	public TaskManager(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, false);
	}
	
	public TaskManager(int corePoolSize, int maximumPoolSize,
		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, boolean testing) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		this.testing = testing;
	}

	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param threadFactory
	 */
	public TaskManager(int corePoolSize, int maximumPoolSize,
		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
		ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
			threadFactory);
		testing = false;
	}

	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param handler
	 */
	public TaskManager(int corePoolSize, int maximumPoolSize,
		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
		RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
			handler);
		testing = false;
	}

	/**
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param workQueue
	 * @param threadFactory
	 * @param handler
	 */
	public TaskManager(int corePoolSize, int maximumPoolSize,
		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
		ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
			threadFactory, handler);
		testing = false;
	}

	/**
	 * Checks the ITask dependency graph for cycles.
	 * 
	 * @return true if a cycle exists, false otherwise.
	 */
	// TODO - determine the threading model to use here
	private boolean cyclesExist() {
		synchronized (taskLock) {
			findHeadTasks();
			if (!headTasks.isEmpty()) {
				Iterator<ITask> iter = headTasks.iterator();
				boolean cycleFound = false;
				while (iter.hasNext() && !cycleFound) {
					ITask task = iter.next();
					cycleFound = checkForCycle(task);
				}
				return cycleFound;
			}
			else  if(taskNameTable.isEmpty()){
					return false;
			}
			else{
				return true;
			}
		}
	}

	/**
	 * Checks given task for a cycle
	 * 
	 * @param task
	 *            The task to start with
	 * @return
	 */
	// TODO - determine the threading model to use here
	private boolean checkForCycle(ITask task) {
		synchronized (taskLock) {
			boolean cycle = false;
			if (!task.isMarked()) {
				// System.out.println("Task " + ((Task)task).getName() + " is
				// unmarked and has " + task.getDependentsCount() + "
				// dependents");
				task.markTask();
				Iterator<ITask> iter = task.getDependents();
				while (iter.hasNext() && !cycle) {
					cycle = checkForCycle(iter.next());
				}
				// Clean up our marks as we retreat up the graph
				task.unmarkTask();

				// System.out.println("Unmarking: " + ((Task)task).getName());
				return cycle;
			}
			else {
				/*
				 * System.out.println("Task " + ((Task) task).getName() + " is
				 * marked and has " + task.getDependentsCount() + "
				 * dependents");
				 */
				return true;
			}
		}
	}

	/**
	 * Resets one or more of the flags/counts in the set of tasks.
	 * 
	 * @param dependencyCount
	 *            the dependency count of the tasks so they match their
	 *            dependency list size
	 * @param marks
	 *            the marked flag used for cycle detection
	 * @param state
	 *            whether the task has been executed
	 */
	private void resetTasks(boolean dependencyCount, boolean marks,
		boolean state) {
		synchronized (taskLock) {
			Enumeration<ITask> elements = taskNameTable.elements();
			while (elements.hasMoreElements()) {
				ITask task = elements.nextElement();
				if (dependencyCount) {
					task.resetDependencyCount();
				}

				if (marks) {
					task.unmarkTask();
				}

				if (state) {
					task.resetState();
				}
			}
		}
	}

	/**
	 * Populates the headTasks vector with all of the ITask objects that have no
	 * dependencies
	 */
	private void findHeadTasks() {
		ITask task = null;
		// make sure any old tasks are cleared out.
		headTasks.clear();
		// clear the headTasks and refind all of the tasks with no dependencies
		Iterator<ITask> iter = headTasks.iterator();
		while (iter.hasNext()) {
			task = iter.next();
			// if the task in the headTasks list now has dependencies, remove it
			if (task.getDependencyCount() > 0) {
				iter.remove();
			}
		}

		Enumeration<String> keys = taskNameTable.keys();
		while (keys.hasMoreElements()) {
			task = taskNameTable.get(keys.nextElement());
			if (task != null) {
				if (task.getDependencyCount() == 0 && !headTasks.contains(task)) {
					headTasks.add(task);
				}
			}
		}
	}

	/**
	 * Adds a single ITask object to the list of tasks as well as all their
	 * dependencies. FIXME: Problem if the task graph is not full developed - a
	 * head task may not remain a head task and vise versa
	 * 
	 * @param task
	 * @throws DuplicateTaskNameException
	 *             If an item with the given name already exists in the graph
	 *             and isn't a placeholder.
	 * @throws IllegalStateException
	 *             If this method is called after {@link #execute(boolean)} has
	 *             been called and has not yet finished.
	 */
	public void addTask(final String name, final Runnable task)
		throws DuplicateTaskNameException, IllegalStateException {
		synchronized (stateLock) {
			if (processing >= 1) {
				throw new IllegalStateException(
					"The TaskManager is currently processing tasks, new tasks cannot be submitted until it is finished.");
			}
		}

		synchronized (taskLock) {
			assert name != null : "Task name cannot be null";
			assert task != null : "Task cannot be null";

			if ((taskNameTable.containsKey(name) && placeholders.contains(name))) {
				// We've seen this name before, but added a placeholder. We
				// need
				// to replace it with the real thing
				ITask realTask = new Task(name, task, this);
				ITask placeholderTask = taskNameTable.get(name);

				if (placeholderTask != null) {
					// grab any dependencies and dependents from the placeholder
					Iterator<ITask> iter = placeholderTask.getDependencies();
					while (iter.hasNext()) {
						ITask dependency = iter.next();
						realTask.addDependency(dependency);
					}

					iter = placeholderTask.getDependents();
					while (iter.hasNext()) {
						ITask dependent = iter.next();
						realTask.addDependent(dependent);
					}

					// Unhook all of our dependents and dependencies from the
					// placeholder
					placeholderTask.removeAllDependencies();
					placeholderTask.removeAllDependents();

					removePlaceHolder(name);
					taskNameTable.put(name, realTask);
				}

			}
			else if (!taskNameTable.containsKey(name)) {
				taskNameTable.put(name, new Task(name, task, this));
			}
			else {
				throw new DuplicateTaskNameException("A task with the name "
					+ name + " already exists.");
			}

		}
	}

	/**
	 * Removes a task from the list of tasks and deletes all dependents'
	 * dependencies on it
	 * 
	 * @param task
	 * @throws IllegalStateException
	 *             When this is attemped while tasks are being processed.
	 */
	public void removeTask(final String name) throws IllegalStateException {
		synchronized (stateLock) {
			if (processing >= 1) {
				throw new IllegalStateException(
					"The TaskManager is currently processing tasks, new tasks cannot be submitted until it is finished.");
			}
		}
		synchronized (taskLock) {
			if (name != null) {
				ITask task = taskNameTable.get(name);
				if (task != null) {
					task.removeAllDependencies();
					task.removeAllDependents();
					taskNameTable.remove(name);
				}
				else {
					throw new NullPointerException("No task by name of " + name
						+ " exists.");
				}
			}
			else {
				throw new NullPointerException("Task name is null");
			}
		}
	}

	/**
	 * Checks to see if any tasks have not been defined yet
	 * 
	 * @return True if any placeholders exist in the graph
	 */
	private boolean tasksNotDefined() {
		if (placeholders.isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * Adds a single dependency to the specified task
	 * 
	 * @param name
	 *            The name of the task to add the dependency to
	 * @param dependency
	 *            The dependency to add. May or may not exist in the task graph
	 *            yet, if not, a placeholder is put there and will be checked
	 *            when the tasks are run to ensure it is defined.
	 * @throws IllegalStateException
	 *             When this method is called while the system is processing
	 *             tasks.
	 */
	public void addDependency(final String name, final String dependency)
		throws IllegalStateException {
		synchronized (stateLock) {
			if (processing >= 1) {
				throw new IllegalStateException(
					"The TaskManager is currently processing tasks, new tasks cannot be submitted until it is finished.");
			}
		}

		synchronized (taskLock) {
			if (name.equals(dependency)) {
				throw new IllegalArgumentException(
					"A task cannot depend on itself!");
			}

			ITask task = taskNameTable.get(name);
			ITask dep = taskNameTable.get(dependency);

			// If the dependency is null, put a placeholder in
			if (dep == null) {
				dep = addPlaceHolder(dependency);
			}

			if (task != null) {
				task.addDependency(dep);
				dep.addDependent(task);
			}
			else {
				throw new NullPointerException("No task with name: " + name
					+ " exists in the graph.");
			}
		}
	}

	/**
	 * Removes a single dependency from the specified task
	 * 
	 * @param name
	 *            The name of the task to remove the dependency from
	 * @param dependency
	 *            The dependency to remove
	 * @throws IllegalStateException
	 *             If this is attempted while tasks are being processed
	 * 
	 * @throws NullPointerException
	 *             if the task with <code>name</code> doesn't exist in the
	 *             graph, or
	 */
	public void removeDependency(final String name, final String dependency)
		throws IllegalStateException {
		synchronized (stateLock) {
			if (processing >= 1) {
				throw new IllegalStateException(
					"The TaskManager is currently processing tasks, new tasks cannot be submitted until it is finished.");
			}
		}
		synchronized (taskLock) {
			ITask task = taskNameTable.get(name);
			ITask dep = taskNameTable.get(dependency);

			if (task != null) {
				if (dep != null) {
					task.removeDependency(dep);
				}
				else {
					throw new NullPointerException(
						"A dependency task name cannot be null.");
				}
			}
			else {
				throw new NullPointerException("A task name cannot be null.");
			}
		}
	}

	/**
	 * Used to add multiple dependencies to a given task
	 * 
	 * @param name
	 *            The name of the task to add dependencies to
	 * @param dependencies
	 *            The dependencies to add
	 * @throws SystemExecutingExeception
	 *             If this change is attempted while tasks are being run.
	 */
	public void addDependencies(final String name, final String[] dependencies)
		throws IllegalStateException {
		for (String dep : dependencies) {
			addDependency(name, dep);
		}
	}

	/**
	 * Convenience method to remove multiple dependencies from a given task
	 * 
	 * @param name
	 *            The name of the task whose dependencies you are removing
	 * @param dependencies
	 *            The dependencies that should be removed
	 * @throws SystemExecutingExeception
	 *             If these changes are attempted while the system is processing
	 *             tasks.
	 */
	public void removeDependencies(final String name,
		final String[] dependencies) throws IllegalStateException {
		for (String dep : dependencies) {
			removeDependency(name, dep);
		}
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor.afterExecute(Runnable
	 *      runnable, Throwable throwable)
	 * 
	 */
	@Override
	public final void afterExecute(Runnable runnable, Throwable throwable) {
		if (runnable instanceof ITask) {
			// If multiple tasks share the same dependent(s), this call may
			// cause their counts to go negative
			ITask task = (ITask) runnable;
			task.notifyDependents();
		}
		if (throwable != null) {
			SLLogger.getLogger().log(testing ? Level.INFO : Level.WARNING, "Problem while executing", throwable);
		}
		
		/* Note that this is mainly to avoid a problem on Linux
		 * where both the uncaught exception handler and the normal afterExecute()
		 * are called
		 */
		if (!(runnable != null && throwable instanceof Error)) {
			eventManager.taskCompleted(runnable);
			//System.out.println("Done with task: "+runnable);
			//new Throwable().printStackTrace(System.out);

			synchronized (stateLock) {
				processing--;
				assert processing >= 0 : "The processing flag should never go below 0.";
			}
			latch.countDown(); // decrement the latch count
		}
	}

	/**
	 * Adds a TaskUncaughtExceptionHandler to the current thread so that Errors
	 * are caught and afterExecute is called. This is to avoid hanging the
	 * system b/c the latch wasn't decremented properly.
	 * 
	 * Also notifies listeners that the given Runnable has started processing.
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor.beforeExecute(Thread thread,
	 *      Runnable runnable)
	 */
	@Override
	public final void beforeExecute(Thread thread, Runnable runnable) {
		Thread.currentThread().setUncaughtExceptionHandler(
			new TaskUncaughtExceptionHandler());
		eventManager.taskStarted(runnable);
		preprocess(runnable);
	}

	@Override
	protected final void terminated() {
	}

	/**
	 * Override hook for specialized subclasses
	 * 
	 * @param runnable
	 */
	protected void preprocess(Runnable runnable) {
		// Default does nothing
	}

	/**
	 * Adds a listener to the list. The listener will be notified of the changes
	 * of running state of this manager.
	 * 
	 * @param listener
	 */
	public void addListener(ITaskListener listener) {
		synchronized (listenerLock) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a listener from the list. The listener will not be notified of
	 * the changes in running state of this manager.
	 * 
	 * @param listener
	 */
	public void removeListener(ITaskListener listener) {
		synchronized (listenerLock) {
			listeners.remove(listener);
		}
	}

	/**
	 * Passes all of the tasks that have no dependencies to the super class to
	 * get run via the normal ThreadPoolExecutor method. Those tasks in turn,
	 * will run their dependents when all of the dependent's dependencies have
	 * been met. If <code>waitForCompletion</code> is <code>true</code> then
	 * this method will wait for a maximum of 180 seconds before returning.
	 * 
	 * @param waitForCompletion
	 *            If true, this method will not return until all tasks have been
	 *            processed
	 * @throws UndefinedDependencyException
	 *             If a dependency was set but the task that it represents
	 *             wasn't
	 * @throws CycleFoundException
	 *             If there is a cyclic dependency in the graph
	 * @throws BrokenBarrierException
	 *             Thrown if the executing thread is interrupted or times out
	 *             while waiting on the barrier.
	 * @throws InterruptedException
	 *             Thrown if the calling thread is interrupted while waiting for
	 *             the executing thread.
	 * @throws TimeoutException
	 *             If the wait for all of the tasks to complete times out
	 * 
	 */
	public void execute(boolean waitForCompletion)
		throws UndefinedDependencyException, CycleFoundException,
		InterruptedException, BrokenBarrierException, TimeoutException {
		execute(waitForCompletion, AWAIT_TIME, TimeUnit.SECONDS);
	}

	/**
	 * Passes all of the tasks that have no dependencies to the super class to
	 * get run via the normal ThreadPoolExecutor method. Those tasks in turn,
	 * will run their dependents when all of the dependent's dependencies have
	 * been met.
	 * 
	 * @param waitForCompletion
	 *            If true, this method will not return until all tasks have been
	 *            processed
	 * 
	 * @param waitTime
	 *            The amount of time to wait for all of the tasks
	 *            to complete. Only used if {@link waitForCompletion} is
	 *            <code>true</code>. The unit for this is determined
	 *            by the {@link waitUnit} parameter.
	 *            
	 * @param waitUnit
	 * 						The unit of measure to use for the waitTime parameter.
	 * 						Also, like the {@link waitTime} parameter, this is only used if
	 * 					 	{@link waitForCompletion} is <code>true</code>.
	 * 
	 * @throws UndefinedDependencyException
	 *             If a dependency was set but the task that it represents
	 *             wasn't
	 * @throws CycleFoundException
	 *             If there is a cyclic dependency in the graph
	 * @throws BrokenBarrierException
	 *             Thrown if the executing thread is interrupted or times out
	 *             while waiting on the barrier.
	 * @throws InterruptedException
	 *             Thrown if the calling thread is interrupted while waiting for
	 *             the executing thread.
	 * @throws TimeoutException
	 *             If the wait for all of the tasks to complete times out
	 */
	public void execute(boolean waitForCompletion, long waitTime, TimeUnit waitUnit)
		throws UndefinedDependencyException, CycleFoundException,
		InterruptedException, BrokenBarrierException, TimeoutException {

		synchronized (stateLock) {
			if (processing >= 1) {
				throw new IllegalStateException(
					"The system is currently processing tasks, you cannot run it again until it has completed.");
			}
			else {
				processing = taskNameTable.size();
			}
		}

		latch = null;

		int parties = 0;
		if (waitForCompletion) {
			parties = taskNameTable.size();
		}
		else {
			parties = 0;
		}
		latch = new CountDownLatch(parties);
		//System.out.println("Done with setup");

		// If we have tasks w/o dependencies, no undefined tasks, and no
		// cycles...
		// Don't have to reset the tasks' counters and markers because each task
		// resets itself in the afterExecute method
		synchronized (taskLock) {
			resetTasks(true, true, true);
			if (!cyclesExist()) {
				if (!tasksNotDefined()) {
					if (!eventManager.isAlive()) {
						eventManager.start();
					}
					eventManager.startingTasks();

					for (ITask task : headTasks) {
						super.execute(task);
					}
				}
				else {
					StringBuffer undefinedTasks = new StringBuffer();
					Iterator<String> iter = placeholders.iterator();
					while (iter.hasNext()) {
						undefinedTasks.append(" " + iter.next() + ",");
					}

					throw new UndefinedDependencyException(
						"The following tasks are undefined: "
							+ undefinedTasks.toString());
				}
			}
			else {
				throw new CycleFoundException(
					"A dependency cycle exists in the graph of ITasks.");
			}
		}

		try {
			if (!latch.await(waitTime, waitUnit)) {
				// we ran out of time
				throw new TimeoutException(
					"Executor timed out while waiting for tasks to finish.");
			}
		}
		catch (InterruptedException e) {
			// This thread was interrupted while waiting
			throw e;
		}

	}

	/**
	 * This should only be called by Task
	 */
	@Override
	public void execute(Runnable runnable) {
		assert false : "This should never be called directly.";
	}

	@Starts("nothing")
	@Override
	public boolean remove(Runnable runnable) {
		assert false : "This should never be called directly.";
		return false;
	}

	/**
	 * Used by Task to immediately add a Task to run.
	 * 
	 * @param task
	 *            The Task to run
	 */
	void executeNow(ITask task) {
		super.execute(task);
	}

	@Starts("nothing")
	@Override
	public void shutdown() {
		if (eventManager != null) {
			eventManager.halt();
		}
		super.shutdown();
	}

	@Starts("nothing")
	@Override
	public List<Runnable> shutdownNow() {
		eventManager.halt();
		return super.shutdownNow();
	}

	/**
	 * Convenience method to add a placeholder - meant to make sure the hash
	 * table and the placeholder list are kept in-sync
	 * 
	 * @param name
	 */
	private ITask addPlaceHolder(final String name) {
		ITask placeholder = new Task(PLACEHOLDER, null, this);
		taskNameTable.put(name, placeholder);
		placeholders.add(name);
		return placeholder;
	}

	/**
	 * Convenience method to remove all traces of a placeholder - meant to make
	 * sure the hash table and the placeholder list are kept in-sync
	 * 
	 * @param name
	 */
	private void removePlaceHolder(final String name) {
		// Replace the placeholder with the real task in the Map
		taskNameTable.remove(name);
		// Remove record of the placeholder in the placeholder list
		placeholders.remove(name);

	}

	/**
	 * Used to check if this TaskManager is currently processing tasks. If any
	 * changes are attempted on a processing TaskManager, an
	 * IllegalStateException is thrown.
	 * 
	 * @return <code>true</code> if any tasks are currently being processed
	 *         (run).
	 */
	public boolean isProcessing() {
		synchronized (stateLock) {
			return (processing >= 1);
		}
	}

	/**
	 * Manages notifying the TaskListeners of new events
	 * 
	 * @author ethan
	 */
	private class TaskEventManager extends Thread {
		private LinkedBlockingQueue<TaskEvent> eventQueue;

		// Tracks the number of tasks completed
		private int completedCount;
		// Tracks the number of tasks started
		private int startedCount;
		// Protects completedCount and startedCount
		private final Object countLock = new Object();

		private int messages = 0;

		// Termination flag
		private boolean running = true;

		public TaskEventManager() {
			eventQueue = new LinkedBlockingQueue<TaskEvent>();
			completedCount = 0;
			startedCount = 0;
		}

		/**
		 * @see java.lang.Thread
		 */
		@Override
    public void run() {
			while (true) {
				try {
					synchronized (this) {
						if (!running && messages == 0) {
							break;
						}
					}
					TaskEvent event = eventQueue.take();

					synchronized (this) {
						--messages;
					}

					EventType type = event.type;

					for (ITaskListener listener : listeners) {
						switch (type) {
						case STARTING:
							listener.startingTasks();
							break;
						case STARTING_TASK:
							listener.taskStarted(event.task);
							break;
						case TASK_COMPLETE:
							listener.taskCompleted(event.task);
							break;
						case DONE:
							listener.allTasksComplete();
							break;
						default:
						}
					}
				}
				catch (InterruptedException e) {
					// Allow to exit
				}
			}
		}

		/**
		 * Called by a client when the processing of tasks has begun
		 * 
		 */
		public void startingTasks() {
			synchronized (this) {
				if (!running) {
					throw new IllegalStateException("EventManage is shutdown");
				}
				++messages;
			}

			try {
				eventQueue.put(new TaskEvent(EventType.STARTING));
				synchronized (countLock) {
					startedCount = 0;
					completedCount = 0;
				}
			}
			catch (InterruptedException e) {
				// Do nothing b/c this is just a notification
			}
		}

		/**
		 * Called by a client when processing of a specific task has begun
		 * 
		 * @param task
		 *            The task that has started processing
		 */
		public void taskStarted(Runnable task) {
			synchronized (this) {
				if (!running) {
					throw new IllegalStateException("EventManager is shutdown.");
				}
				++messages;
			}
			try {
				eventQueue.put(new TaskEvent(EventType.STARTING_TASK, task));
				synchronized (countLock) {
					startedCount++;
				}
			}
			catch (InterruptedException e) {
				// Do nothing b/c this is just a notification
			}
		}

		/**
		 * Called by a client when processing of a specific task has completed
		 * 
		 * @param task
		 *            The task that has finished being processed
		 */
		public void taskCompleted(Runnable task) {
			synchronized (this) {
				if (!running) {
					throw new IllegalStateException("EventManager is shutdown.");
				}
				++messages;
			}

			try {
				eventQueue.put(new TaskEvent(EventType.TASK_COMPLETE, task));
				synchronized (countLock) {
					completedCount++;
					if (completedCount == taskNameTable.size()
						&& completedCount == startedCount) {
						allTasksComplete();
					}
				}

			}
			catch (InterruptedException e) {
				// Do nothing b/c this is just a notification
				SLLogger.getLogger().log(Level.FINE, "Interrupted", e);
			}
		}

		/**
		 * Called by a client when processing of all tasks has completed
		 * 
		 */
		private void allTasksComplete() {
			try {
				synchronized (this) {
					++messages;
				}
				// System.out.println("All Tasks Complete");
				eventQueue.put(new TaskEvent(EventType.DONE));
			}
			catch (InterruptedException e) {
				// Do nothing b/c this is just a notification
				SLLogger.getLogger().log(Level.FINE, "Interrupted", e);
			}
		}

		/**
		 * Called by a client when processing of a specific task has begun
		 * 
		 */
		@Starts("nothing")
		public void halt() {
			synchronized (this) {
				running = false;
			}
			interrupt();
		}
	}

	/**
	 * Enum for use with TaskEvent
	 * 
	 * @author ethan
	 */
	protected enum EventType {
		STARTING, STARTING_TASK, TASK_COMPLETE, DONE
	}

	/**
	 * A class that is used by the TaskEventManager to determine what to call in
	 * the listeners
	 * 
	 * @author ethan
	 */
	protected static class TaskEvent {
		public final Runnable task;
		public final EventType type;

		public TaskEvent(EventType type) {
			this(type, null);
		}

		public TaskEvent(EventType type, Runnable task) {
			this.type = type;
			this.task = task;
		}

	}

	/**
	 * An implementation of {@link UncaughtExceptionHandler} to ensure our
	 * execute() method doesn't hang in the case that an Error is thrown.
	 * 
	 * <em>WARNING</em>: This will only be helpful if the Task that's running
	 * is not depending on
	 * 
	 * @author ethan
	 */
	public class TaskUncaughtExceptionHandler implements
		Thread.UncaughtExceptionHandler {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread,
		 *      java.lang.Throwable)
		 * XXX: This passes a null to afterExecute, which expects a Runnable. Currently that method checks for
		 * the type of the parameter passed in so this is safe. However, it is good to be aware of this. The Task
		 * itself also catches this error and uses that to notify its dependents so that part of afterExecute() is
		 * still performed, albeit at a different point.
		 */
		@Override
    public void uncaughtException(Thread t, Throwable e) {
			// Error is the only case that afterExecute will not be run
			if (e instanceof Error) {
				afterExecute(null, e);
			}
		}
	}
}
