/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaUnparser;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 *
 */
public abstract class AbstractPromiseStorageAndCheckRule<T>
extends AbstractNamedPromiseRule
implements IPromiseCheckRule, IPromiseStorage<T>, IPromiseUnparseRule
{
  final int type;
  final Operator[] storOps;
  // SlotInfo si;

	protected AbstractPromiseStorageAndCheckRule(String n, int t, Operator[] checkOps, Operator[] storOps) {
    super(n, checkOps);
    this.storOps = storOps;
    type = t;
	}

	protected AbstractPromiseStorageAndCheckRule(String n, int t, Operator[] ops) {
		this(n, t, PromiseConstants.noOps, ops);    
	}  	
	
  protected AbstractPromiseStorageAndCheckRule(String n, int t, Operator op) {
    this(n, t, new Operator[] {op});    
  }  

  protected AbstractPromiseStorageAndCheckRule(String n, int t) {
    this(n, t, PromiseConstants.noOps);    
  } 

	@Override
  public final Operator[] getOps(Class type) {
		if (type == IPromiseStorage.class) { 
			return storOps;
		}
		return super.getOps(type);
	}  
  
  public final int type() {
    return type;
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.bind.IPromiseCheckRule#checkSanity(edu.cmu.cs.fluid.tree.Operator, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.bind.IPromiseCheckReport)
	 */
	public boolean checkSanity(Operator op, IRNode promisedFor, IPromiseCheckReport report) {
		// Do nothing
    return true;
	}
  
	public void unparse(final IRNode node, final JavaUnparser u) {
    // TODO what to unparse?
	  switch (type) {
		case BOOL:
		case INT:
		case NODE:
		case SEQ:
	  default:
	  }
	}
}
