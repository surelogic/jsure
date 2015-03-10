/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultSLColorAnnotationParseRule.java,v 1.1 2007/10/24 15:18:10 dfsuther Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.SLThreadRoleAnnotationsParser;
import com.surelogic.annotation.parse.SLThreadRoleParse;
//import com.surelogic.annotation.rules.ThreadRoleRules;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.tree.Operator;

public abstract class DefaultSLThreadRoleAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<? super A>>
extends AbstractAntlrParseRule<A, P, SLThreadRoleAnnotationsParser>  {
  protected DefaultSLThreadRoleAnnotationParseRule(String name, Operator[] ops,
      Class<A> dt, AnnotationLocation loc) {
      super(name, ops, dt, loc);
    }

    protected DefaultSLThreadRoleAnnotationParseRule(String name, Operator[] ops,
      Class<A> dt) {
      this(name, ops, dt, AnnotationLocation.DECL);
    }

    @Override
    protected SLThreadRoleAnnotationsParser initParser(String contents) throws Exception {
      return SLThreadRoleParse.prototype.initParser(contents);
    }

    @Override
    protected final Object parse(IAnnotationParsingContext context,
            SLThreadRoleAnnotationsParser parser) throws Exception, RecognitionException {
    	/*
    	if (!ThreadRoleRules.useThreadRoles) {
    		return ParseResult.IGNORE;
    	}
    	*/
    	return parseTRoleAnno(context, parser);
    }
    
    protected abstract Object parseTRoleAnno(IAnnotationParsingContext context,
        SLThreadRoleAnnotationsParser parser) throws Exception, RecognitionException;  
}
