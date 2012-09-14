/*
 * $Header:
 * /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultSLAnnotationParseRule.java,v
 * 1.12 2007/07/09 18:40:14 chance Exp $
 */
/*
 * $Header:
 * /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultSLAnnotationParseRule.java,v
 * 1.13 2007/07/18 14:30:51 chance Exp $
 */
package com.surelogic.annotation;

import com.surelogic.aast.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.tree.Operator;

/**
 * Abstract class for annotations parsed using the standard parser
 * (SLAnnotationsParser)
 * 
 * @author Edwin.Chan
 * @see com.surelogic.annoation.parse.SLAnnotations.g
 */
public abstract class DefaultSLAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<? super A>>
	extends AbstractAntlrParseRule<A, P, SLAnnotationsParser> {
	protected DefaultSLAnnotationParseRule(String name, Operator[] ops,
		Class<A> dt, AnnotationLocation loc) {
		super(name, ops, dt, loc);
	}

	protected DefaultSLAnnotationParseRule(String name, Operator[] ops,
		Class<A> dt) {
		this(name, ops, dt, AnnotationLocation.DECL);
	}

	@Override
	protected SLAnnotationsParser initParser(String contents) throws Exception {
	  return SLParse.prototype.initParser(contents);
	}
}
