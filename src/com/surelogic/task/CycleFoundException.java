/*$Header: /cvs/fluid/fluid/src/com/surelogic/task/CycleFoundException.java,v 1.1 2007/06/22 19:32:01 ethan Exp $*/
package com.surelogic.task;

/**
 * TODO Fill in purpose.
 * @author ethan
 */
public class CycleFoundException extends Exception {

	/**
	 * 
	 */
	public CycleFoundException() {
		super();
	}

	/**
	 * @param message
	 */
	public CycleFoundException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public CycleFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CycleFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
