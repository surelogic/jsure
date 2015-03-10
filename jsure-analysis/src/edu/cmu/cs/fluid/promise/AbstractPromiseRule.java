/*
 * Created on Oct 30, 2003
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.tree.Operator;

/**
 * A basic implementation for rules that need to specify what Operators
 * they apply to.
 * 
 * @author chance
 */
public abstract class AbstractPromiseRule implements IPromiseRule
{ 
  protected final Operator[] ops;
	
	protected AbstractPromiseRule(Operator op) {
    ops = new Operator[] {op};    
  }

	protected AbstractPromiseRule(Operator[] ops) {
		this.ops = ops;
	}
	
	@Override
  public Operator[] getOps(Class type) {
		return ops;
	}
}
