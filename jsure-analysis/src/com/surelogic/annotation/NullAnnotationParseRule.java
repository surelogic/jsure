package com.surelogic.annotation;

import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.AbstractNamedPromiseRule;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * This rule does nothing by itself
 * 
 * @author Edwin.Chan
 */
@SuppressWarnings("rawtypes")
public class NullAnnotationParseRule extends AbstractNamedPromiseRule implements IAnnotationParseRule {
  protected NullAnnotationParseRule(String name, Operator[] ops) {
    super(name, ops);
  }

  public NullAnnotationParseRule(String name) {
    super(name, anyOp);
  }

  @Override
  public boolean declaredOnValidOp(Operator declaredOn) {
    for (Operator op : ops) {
      if (op.includes(declaredOn)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public IAnnotationScrubber getScrubber() {
    return null;
  }

  @Override
  public IPromiseDropStorage getStorage() {
    return null;
  }

  @Override
  public ParseResult parse(IAnnotationParsingContext context, String contents) {
    return ParseResult.IGNORE;
  }

  @Override
  public boolean appliesTo(IRNode decl, Operator op) {
    return declaredOnValidOp(op);
  }
}
