/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/IAnnotationScrubber.java,v 1.6 2007/08/21 16:42:31 chance Exp $*/
package com.surelogic.annotation.scrub;

/**
 * Encapsulates the scrubber code, and specifies its dependencies on 
 * other scrubbers.
 * 
 * Runnable.run() executes the scrubber.
 * 
 * @author Edwin.Chan
 */
public interface IAnnotationScrubber extends Runnable {
  String[] NONE = {};
  
  /**
   * @return The name to use for the scrubber framework
   */
  String name();
  
  /**
   * Returns whether this runs first, last, or in the middle
   */
  ScrubberOrder order();
  
  /**
   * @return The names of the scrubbers that this depends on 
   */
  String[] dependsOn();
  
  /**
   * @return The names of the scrubbers that this should run before
   */
  String[] shouldRunBefore();
  
  /**
   * Set the context for reporting errors
   */
  void setContext(IAnnotationScrubberContext context);
}
