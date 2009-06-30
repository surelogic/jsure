/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/SimpleBooleanAnnotationParseRule.java,v 1.2 2007/07/20 14:53:10 chance Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.SLAnnotationsParser;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A parse rule that expects to parse nothing
 * 
 * @author Edwin.Chan
 */
public class SimpleBooleanAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<A>> 
extends DefaultBooleanAnnotationParseRule<A,P> {
  protected SimpleBooleanAnnotationParseRule(String name, Operator[] ops, Class<A> dt) {
    super(name, ops, dt);
  }

  @Override
  protected final Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
    return parser.nothing().getTree();
  }
}
