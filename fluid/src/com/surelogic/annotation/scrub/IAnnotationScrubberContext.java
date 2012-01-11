/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/IAnnotationScrubberContext.java,v 1.5 2007/10/23 17:50:50 aarong Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

/**
 * Context object for 
 * 
 * @author Edwin.Chan
 */
public interface IAnnotationScrubberContext {
  
  IBinder getBinder(IRNode context);
  
  /**
   * Report error on the same IRNode that the proposal is set for
   */
  void reportErrorAndProposal(ProposedPromiseDrop p, String msgTemplate, Object... args);
  void reportError(IRNode n, String msgTemplate, Object... args);
  
  void reportError(IAASTNode n, String msgTemplate, Object... args);
  void reportError(String msg, IAASTNode n);
  
  void reportWarning(IAASTNode n, String msgTemplate, Object... args);
  void reportWarning(String msg, IAASTNode n);
}
