/*
 * Created on Nov 4, 2003
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Callback interface for reporting warnings and errors, as we parse promises
 * @author chance
 */
public interface IPromiseCheckReport {
	/**
	 * 
	 * @param description
	 * @param promise Could be null if boolean
	 */
  void reportWarning(String description, IRNode promise);    

	/**
	 * 
	 * @param description
	 * @param promise Could be null if boolean
	 */
  void reportError(String description, IRNode promise);  
}
