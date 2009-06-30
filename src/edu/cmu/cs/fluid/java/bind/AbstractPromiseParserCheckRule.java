/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

// import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.parse.AbstractPromiseParserRule;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 *
 */
public abstract class AbstractPromiseParserCheckRule<T> extends AbstractPromiseParserRule
implements IPromiseCheckRule, IPromiseStorage<T>
{
	/**
	 * Logger for this class
	 */
	static final Logger LOG = SLLogger.getLogger("ECLIPSE.fluid.promise");

	final Operator[] checkOps;
	final Operator[] storOps;
	int type;

	protected AbstractPromiseParserCheckRule(String tag, int t, boolean multi,
                                           Operator parseOp, Operator checkOp) {
		this(tag, t, multi, parseOp, checkOp, checkOp);    
	}
	
	protected AbstractPromiseParserCheckRule(String tag, int t, boolean multi, 
                                           Operator[] parseOps, Operator[] checkOps) {
		this(tag, t, multi, parseOps, checkOps, checkOps);    
	}	
  
  protected AbstractPromiseParserCheckRule(String tag, int t, boolean multi,
                                           Operator parseOp, Operator checkOp, Operator storOp) {
    this(tag, t, multi, new Operator[] {parseOp}, new Operator[] {checkOp}, new Operator[] {storOp});    
  }

	protected AbstractPromiseParserCheckRule(String tag, int t, boolean multi,
																					 Operator[] parseOps, Operator[] checkOps, Operator[] storOps) {
		super(tag, t!=SEQ? false : multi, parseOps);
    type = t;
    if (t != SEQ && multi) {
      LOG.severe("A rule with type != SEQ should not have multiple IRNode results");
    }
		this.checkOps = checkOps;
		this.storOps  = storOps;
		
		if (parseOps.length == 0) {
			LOG.severe("No acceptable parse operators for @"+name);
		}
		if (checkOps.length == 0) {
			LOG.warning("No acceptable check operators for @"+name);
		}
		if (storOps.length == 0) {
			LOG.warning("No acceptable storage operators for @"+name);
		}
	}
	
	@Override
  public final Operator[] getOps(Class type) {
		if (type == IPromiseStorage.class) { 
			return storOps;
		}
		else if (type == IPromiseCheckRule.class) { 
  		return checkOps;
		}
		return super.getOps(type);
	}

  public final int type() {
    return type;
  }

  public boolean checkSanity(Operator op, IRNode promisedFor, IPromiseCheckReport report) {
    // Do nothing
    return true;
  }
}
