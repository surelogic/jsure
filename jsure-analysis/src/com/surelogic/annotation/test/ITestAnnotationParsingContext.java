/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/test/ITestAnnotationParsingContext.java,v 1.4 2007/08/02 13:29:17 chance Exp $*/
package com.surelogic.annotation.test;

import com.surelogic.annotation.IAnnotationParsingContext;

public interface ITestAnnotationParsingContext 
extends IAnnotationParsingContext 
{
  void setTestResultForUpcomingPromise(TestResultType r, String explanation);
   
  /**
   * For the next promise
   */
  void setTestResultForUpcomingPromise(TestResultType r, String context, String explanation);
}
