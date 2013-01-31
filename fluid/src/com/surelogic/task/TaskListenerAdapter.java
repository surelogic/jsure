/*$Header: /cvs/fluid/fluid/src/com/surelogic/task/TaskListenerAdapter.java,v 1.1 2007/06/25 21:31:45 ethan Exp $*/
package com.surelogic.task;

/**
 * TODO Fill in purpose.
 * @author ethan
 */
public class TaskListenerAdapter implements ITaskListener {

	/* (non-Javadoc)
	 * @see com.surelogic.task.ITaskListener#allTasksComplete()
	 */
	@Override
  public void allTasksComplete() {

	}

	/* (non-Javadoc)
	 * @see com.surelogic.task.ITaskListener#startingTasks()
	 */
	@Override
  public void startingTasks() {

	}

	/* (non-Javadoc)
	 * @see com.surelogic.task.ITaskListener#taskCompleted(java.lang.Runnable)
	 */
	@Override
  public void taskCompleted(Runnable task) {

	}

	/* (non-Javadoc)
	 * @see com.surelogic.task.ITaskListener#taskStarted(java.lang.Runnable)
	 */
	@Override
  public void taskStarted(Runnable task) {

	}

}
