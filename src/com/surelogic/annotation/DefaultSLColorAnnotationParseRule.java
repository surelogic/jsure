/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultSLColorAnnotationParseRule.java,v 1.1 2007/10/24 15:18:10 dfsuther Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.SLColorAnnotationsParser;
import com.surelogic.annotation.parse.SLColorParse;
import com.surelogic.annotation.rules.ColorRules;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class DefaultSLColorAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<? super A>>
extends AbstractAntlrParseRule<A, P, SLColorAnnotationsParser>  {
  protected DefaultSLColorAnnotationParseRule(String name, Operator[] ops,
      Class<A> dt, AnnotationLocation loc) {
      super(name, ops, dt, loc);
    }

    protected DefaultSLColorAnnotationParseRule(String name, Operator[] ops,
      Class<A> dt) {
      this(name, ops, dt, AnnotationLocation.DECL);
    }

    @Override
    protected SLColorAnnotationsParser initParser(String contents) throws Exception {
      return SLColorParse.prototype.initParser(contents);
    }

    @Override
    protected final Object parse(IAnnotationParsingContext context,
            SLColorAnnotationsParser parser) throws Exception, RecognitionException {
    	if (!ColorRules.useColoring) {
    		return ParseResult.IGNORE;
    	}
    	return parseColorAnno(context, parser);
    }
    
    protected abstract Object parseColorAnno(IAnnotationParsingContext context,
        SLColorAnnotationsParser parser) throws Exception, RecognitionException;  
}
