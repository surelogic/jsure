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
public interface IBooleanPromiseHandler extends IPromiseHandler {
  /**
   * @param n The node that has the given boolean promise
   * @param info Info on how the promise is stored
   */
  void processBooleanPromise(IRNode n, TokenInfo<?> info);
}
