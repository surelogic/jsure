/*
 * Created on Dec 17, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

/**
 * T specifies the result type
 * @author chance
 */
public interface ITypeSearchStrategy<T> {
	/**
	 * Logger for this class
	 */
	static final Logger LOG = SLLogger.getLogger("ECLIPSE.fluid.bind");

	/* Returns a constant string representing what this strategy is searching for */
	String getLabel();

	/*
	 * @return Null if no result
	 */
	T getResult();
}
