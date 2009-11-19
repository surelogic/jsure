/*
 * Created on Nov 20, 2003
 *
 */
package edu.cmu.cs.fluid.promise.parse;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 */
public abstract class AbstractParseRule extends AbstractNamedPromiseRule implements IPromiseParseRule, PromiseConstants {
	/**
	 * Logger for this class
	 */
	protected static final Logger LOG = SLLogger.getLogger("ECLIPSE.fluid.promise");
	
	protected AbstractParseRule(String name, Operator op) {
		super(name, op);
	}
  protected AbstractParseRule(String name, Operator[] ops) {
		super(name, ops);
	}
	
  /**
   * Wrapper to do checking on the promisedFor node.
   * 
   * @return Returns the node to be annotated
   */
  protected final IRNode getPromisedFor(IRNode n, String contents, IPromiseParsedCallback cb) {
		if (n == null) {
			cb.noteProblem("Null promisedOn node for @"+name+" "+contents);
			return null;
		}
    Operator op    = tree.getOperator(n);
    String problem = checkPromisedOn(n, op);
    if (problem != null) {
      // TODO Should this also mark it as invalid?
      cb.noteProblem(problem);
      return null;
    }
    return getPromisedFor_raw(n, contents, cb);
  }
	
  /**
   * 
   * @param promisedOn
   * @return Description of the problem, if any; otherwise null
   */
  protected final String checkPromisedOn(IRNode promisedOn, Operator op) {
		for(int i=0; i<ops.length; i++) {
			if (ops[i].includes(op)) {
				return null;
			}
		}
		StringBuilder names = new StringBuilder(ops[0].name());
		for(int i=1; i<ops.length; i++) {
			names.append(", ");
			names.append(ops[i].name());
		}
		return "@"+name+" cannot annotate "+op.name()+" -- Needs one of "+names;    
	}

	/**
	 * @return Returns the actual node to be annotated
	 */
	protected IRNode getPromisedFor_raw(IRNode n, String contents, IPromiseParsedCallback cb) {
		return n;
	}
  
  /**
   * Handle a boolean promise on a single promisedFor node
   */
  protected final boolean parseBoolean(IRNode n, String contents, IPromiseParsedCallback cb) {
    if (contents != null && !contents.equals("")) {
      cb.noteProblem("Ignoring the tag contents: " + contents);
    }
    IRNode p = getPromisedFor(n, contents, cb);
    if (p != null) {
      // p.setSlotValue(getSI(), Boolean.TRUE);    
      AbstractPromiseAnnotation.setX_mapped(getSI(), p, true); 
      parsedSuccessfully(p);
      cb.parsed();
      return true;
    }
    return false;
  }
  
  protected SlotInfo<Boolean> getSI() {
    return null;
  }
  
  /**
   * Called if the promise is parsed successfully 
   */
  protected void parsedSuccessfully(IRNode promisedFor) {
	  // Nothing to do
  }
}
