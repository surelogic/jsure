/*
 * Created on Oct 10, 2003
 *  
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.util.OpSearch;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 *  
 */
public abstract class AbstractPromiseBindRule
  extends AbstractPromiseRule
  implements IPromiseBindRule, PromiseConstants {
  private Logger LOG = BIND;
  
  protected ITypeEnvironment tEnv;

  AbstractPromiseBindRule(Operator op) {
    super(op);
  }

  AbstractPromiseBindRule(Operator[] ops) {
    super(ops);
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.java.bind.IPromiseBindRule#getBinding(edu.cmu.cs.fluid.tree.Operator,
	 *      edu.cmu.cs.fluid.ir.IRNode)
	 * 
	 * op and use are assumed non-null
	 */
  public final IRNode getBinding(Operator op, IRNode use) {
    IRNode result = bind(op, use);

    if (LOG.isLoggable(Level.FINE)
      && result != null
      && op instanceof IHasBinding) {
      IHasBinding b = (IHasBinding) op;
      Operator rop = tree.getOperator(result);
      if (!b.getResultOp().includes(rop)) {
        LOG.fine("Trying to bind " + DebugUnparser.toString(use));
        LOG.fine("Got back " + DebugUnparser.toString(result));
      }
    }
    return result;
  }

  protected abstract IRNode bind(Operator op, IRNode use);

  protected final IRNode findNamedType(ITypeEnvironment tEnv, final IRNode n, final String name) {
    IRNode type = tEnv.findNamedType(name);
    if (type == null && name.indexOf(".") < 0) {
      // Unqualified name, so use local environment (java.lang or imports)

      // TODO This doesn't work for promises!
      final IRNode cu = OpSearch.cuSearch.findEnclosing(n);
      final LocalEnvironment le = LocalEnvironment.createCUenv(tEnv, cu);
      return le.findSimpleType(name);
    }
    return type;
  }

  /**
	 * (Copied) Finds the first non-null promised-for node up the chain
	 */
  protected final IRNode getPromisedFor(IRNode n) {
    if (n == null) {
      LOG.fine("getPromisedFor got and returned null");
      return null;
    }
    do {
			IRNode subject = JavaPromise.getPromisedForOrNull(n);
			if (subject != null) {
				LOG.fine("getPromisedFor got something and is returning it");
				return subject;
			}
			LOG.fine("getPromisedFor is looking at my parent");
			n = tree.getParentOrNull(n);
    } while (n != null);
    
    LOG.severe("No promised-for node for " + DebugUnparser.toString(n));
    return null;
  }
}
