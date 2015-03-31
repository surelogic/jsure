/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/IAnnotationScrubberContext.java,v 1.5 2007/10/23 17:50:50 aarong Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.*;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.irfree.DiffHeuristics;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Context object for 
 * 
 * @author Edwin.Chan
 */
public interface IAnnotationScrubberContext {
  int UNKNOWN = DiffHeuristics.UNKNOWN;

  IBinder getBinder(IRNode context);
  
  /**
   * Report error on the same IRNode that the proposal is set for
   */
  IModelingProblemDrop reportErrorAndProposal(ProposedPromiseDrop p, String msgTemplate, Object... args);
  IModelingProblemDrop reportErrorAndProposal(ProposedPromiseDrop p, int number, Object... args);
  
  IModelingProblemDrop reportError(IRNode n, String msgTemplate, Object... args);
  IModelingProblemDrop reportError(IRNode n, int number, Object... args);
  
  IModelingProblemDrop reportError(IAASTNode n, int number, Object... args);
  IModelingProblemDrop reportError(IAASTNode n, String msgTemplate, Object... args);
  IModelingProblemDrop reportError(String msg, IAASTNode n);
  
  IModelingProblemDrop reportWarning(IAASTNode n, int number, Object... args);
  IModelingProblemDrop reportWarning(String msg, IAASTNode n);
  IModelingProblemDrop reportWarningAndProposal(ProposedPromiseDrop p, String msgTemplate, Object... args);
}