/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/IAnnotationScrubberContext.java,v 1.5 2007/10/23 17:50:50 aarong Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Context object for 
 * 
 * @author Edwin.Chan
 */
public interface IAnnotationScrubberContext {
  
  IBinder getBinder();
  
  void reportError(IAASTNode n, String msgTemplate, Object... args);
  void reportError(String msg, IAASTNode n);
  
  void reportWarning(IAASTNode n, String msgTemplate, Object... args);
  void reportWarning(String msg, IAASTNode n);
}
