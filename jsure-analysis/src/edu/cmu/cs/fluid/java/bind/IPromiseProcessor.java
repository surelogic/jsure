/*
 * Created on Aug 29, 2004
 *
 */
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.annotation.*;


/**
 * @author Edwin
 *
 */
public interface IPromiseProcessor extends IBooleanPromiseHandler, INodePromiseHandler, ISequencePromiseHandler 
{
  /**
   * @return true if we want the framework to continue examining promises
   */
  boolean continueProcessing();

  /**
   * Called on ReceiverDecls and QualifiedReceiverDecl
   * @param receiver
   * @return
   */
  IPromiseProcessor getProcessorForReceiver(IRNode n, IRNode receiver);

  /**
   * @param retnode
   * @return
   */
  IPromiseProcessor getProcessorForReturnNode(IRNode n, IRNode retnode);
}
