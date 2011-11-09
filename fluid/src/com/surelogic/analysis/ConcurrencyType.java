package com.surelogic.analysis;

/**
 * Describes how the analysis handles the parallelism
 * 
 * @author Edwin
 */
public enum ConcurrencyType {
	NEVER, 
	/**
	 * The infrastructure runs the analysis in parallel by CU
	 */
	EXTERNALLY,
	/**
	 * The analysis handles its own parallelism
	 */
	INTERNALLY
}
