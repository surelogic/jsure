/*
 * Created on Sep 13, 2004
 *
 */
package edu.cmu.cs.fluid.java.annotation;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;


/**
 * @author Edwin
 *
 */
public interface INodePromiseHandler extends IPromiseHandler {
  /**
   * @param n The node that has the given node promise
   * @param info Info on how the promise is stored
   * @param sub The IRNode representation of the promise
   */
  void processNodePromise(IRNode n, TokenInfo info, IRNode sub);
}
