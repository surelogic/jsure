/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/SimpleBooleanAnnotationParseRule.java,v 1.2 2007/07/20 14:53:10 chance Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.tree.Operator;

/**
 * A parse rule that normally does not parse anything
 * Now specialized for annotations on types like @ThreadSafe
 * 
 * @author Edwin.Chan
 */
public class SimpleBooleanAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<A>> 
extends DefaultBooleanAnnotationParseRule<A,P> {
  protected SimpleBooleanAnnotationParseRule(String name, Operator[] ops, Class<A> dt) {
    super(name, ops, dt);
  }

  @Override
  protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
	if (context.getSourceType() == AnnotationSource.JAVA_5 && !context.getAllText().isEmpty()) {		
		// Only possible for scoped promises
		// Try to parse the contents
		return parser.annoParameters().getTree();
	}
    return parser.nothing().getTree();
  }
  
  @Override
  protected AnnotationLocation translateTokenType(int type, Operator op) {
	if (type == SLAnnotationsParser.AnnoParameters) {
		return AnnotationLocation.DECL;
	}
	return super.translateTokenType(type, op);
  }
}
