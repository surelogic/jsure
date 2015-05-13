/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractNodePromiseProcessor.java,v 1.4 2007/07/05 18:15:15 aarong Exp $*/
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;

/**
 * Calls process() on each IRNode promise hanging off the node
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractNodePromiseProcessor extends AbstractPromiseProcessor {
  @Override
  public boolean continueProcessing() {
    return true;
  }

  @Override
  public IPromiseProcessor getProcessorForReceiver(IRNode n, IRNode receiver) {
    process(n, receiver);
    return this;
  }

  @Override
  public IPromiseProcessor getProcessorForReturnNode(IRNode n, IRNode retnode) {
    process(n, retnode);
    return this;
  }

  @Override
  public void processNodePromise(IRNode n, TokenInfo<?> info, IRNode sub) {
    process(n, sub);
  }

  @Override
  public void processSequencePromise(IRNode n, TokenInfo<?> info, Iteratable<IRNode> e) {
    for (IRNode sub : e) {
      if (sub != null) {
        process(n, sub);
      }
    }
  }

  protected void process(IRNode promisedFor, IRNode n) {
    process(n);
  }

  protected abstract void process(IRNode n);
}
