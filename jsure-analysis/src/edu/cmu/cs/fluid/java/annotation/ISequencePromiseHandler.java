/*
 * Created on Sep 13, 2004
 *
 */
package edu.cmu.cs.fluid.java.annotation;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;


/**
 * @author Edwin
 *
 */
public interface ISequencePromiseHandler extends IPromiseHandler {

  /**
   * @param n The node that has the given sequence promise
   * @param info Info on how the promise is stored
   * @param e An enumeration based off of the original IRSequence
   */
  void processSequencePromise(IRNode n, TokenInfo info, Iteratable<IRNode> e);
}
