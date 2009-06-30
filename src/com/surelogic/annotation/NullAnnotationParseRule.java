/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/NullAnnotationParseRule.java,v 1.2 2007/09/27 15:07:34 chance Exp $*/
package com.surelogic.annotation;

import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.promise.AbstractNamedPromiseRule;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * This rule does nothing by itself
 * @author Edwin.Chan
 */
@SuppressWarnings("unchecked")
public class NullAnnotationParseRule extends AbstractNamedPromiseRule implements
    IAnnotationParseRule {
  protected NullAnnotationParseRule(String name, Operator[] ops) {
    super(name, ops);
  }
  public NullAnnotationParseRule(String name) {
    super(name, anyOp);
  }

  public boolean declaredOnValidOp(Operator op) {
    return true;
  }
  
  public IAnnotationScrubber getScrubber() {
    return null;
  }

  public IPromiseDropStorage getStorage() {
    return null;
  }

  public void parse(IAnnotationParsingContext context, String contents) {
    // do nothing
  }
}
