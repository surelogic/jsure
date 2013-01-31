package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.util.Iteratable;

public abstract class AbstractPromiseProcessor implements IPromiseProcessor {
  @Override
  public boolean continueProcessing() {
    return false;
  }

  @Override
  public IPromiseProcessor getProcessorForReceiver(IRNode n, IRNode receiver) {
    return null;
  }

  @Override
  public IPromiseProcessor getProcessorForReturnNode(IRNode n, IRNode retnode) {
    return null;
  }

  @Override
  public void processBooleanPromise(IRNode n, TokenInfo info) {
  }

  @Override
  public void processNodePromise(IRNode n, TokenInfo info, IRNode sub) {
  }

  @Override
  public void processSequencePromise(IRNode n, TokenInfo info,
      Iteratable<IRNode> e) {
  }
}
