package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.util.Iteratable;

public abstract class AbstractPromiseProcessor implements IPromiseProcessor {
  public boolean continueProcessing() {
    return false;
  }

  public IPromiseProcessor getProcessorForReceiver(IRNode n, IRNode receiver) {
    return null;
  }

  public IPromiseProcessor getProcessorForReturnNode(IRNode n, IRNode retnode) {
    return null;
  }

  public void processBooleanPromise(IRNode n, TokenInfo info) {
  }

  public void processNodePromise(IRNode n, TokenInfo info, IRNode sub) {
  }

  public void processSequencePromise(IRNode n, TokenInfo info,
      Iteratable<IRNode> e) {
  }
}
