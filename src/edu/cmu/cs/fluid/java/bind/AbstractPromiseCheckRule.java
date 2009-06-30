/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 */
public abstract class AbstractPromiseCheckRule 
extends AbstractNamedPromiseRule
implements IPromiseCheckRule
{
	protected AbstractPromiseCheckRule(String n, Operator[] checkOps) {
    super(n, checkOps);
	}
	
  protected AbstractPromiseCheckRule(String n, Operator op) {
    this(n, new Operator[] {op});    
  }  

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.bind.IPromiseCheckRule#checkSanity(edu.cmu.cs.fluid.tree.Operator, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.bind.IPromiseCheckReport)
	 */
	public boolean checkSanity(Operator op, IRNode promisedFor, IPromiseCheckReport report) {
		// Do nothing
    return true;
	}
}
