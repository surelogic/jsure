/*
 * Created on Mar 23, 2004
 *
 */
package edu.cmu.cs.fluid.promise;

import java.util.Iterator;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @author chance
 */
public interface IPromiseParser {
	/**
	 * @param rule
	 * @return
	 */
	IPromiseParseRule addRule(IPromiseParseRule rule);

	/**
	 * @param target
	 * @param promise
	 * @param callback
	 */
	boolean parsePromise(IRNode target, String promise, IPromiseParsedCallback callback);
	
	IPromiseParseRule getRule(String promise);
	
	Iterator<IPromiseParseRule> getRules();
}
