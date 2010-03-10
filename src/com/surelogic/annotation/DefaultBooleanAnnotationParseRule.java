/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultBooleanAnnotationParseRule.java,v 1.13 2008/11/17 18:22:17 chance Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.AASTAdaptor;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.parse.SLParse;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Abstract class for boolean parse rules 
 * (e.g. @Unique)
 * 
 * @author Edwin.Chan
 */
public abstract class DefaultBooleanAnnotationParseRule
<A extends IAASTRootNode, P extends PromiseDrop<A>>  
extends AbstractAnnotationParseRule<A,P> {
  protected DefaultBooleanAnnotationParseRule(String name, Operator[] ops, Class<A> dt) {
    super(name, ops, dt);
  }

  protected DefaultBooleanAnnotationParseRule(String name, Operator op, Class<A> dt) {
    super(name, op, dt);
  }
  
  /**
   * Assumes that parsed text specifies where the annotations should go
   * Handles differences between syntax for Java 5 and Javadoc
   */
  public final ParseResult parse(IAnnotationParsingContext context, String contents) {
    if (!declaredOnValidOp(context.getOp())) {
      context.reportError(IAnnotationParsingContext.UNKNOWN,
                          "Promise declared on invalid operator");
      return ParseResult.FAIL;
    }
    try {
      AASTAdaptor.Node tn    = (AASTAdaptor.Node) parse(context, SLParse.prototype.initParser(contents));
      if (tn == null) {
        return ParseResult.FAIL;
      }
      if (tn.getType() == SLAnnotationsParser.Expressions) {
        /*
        if (context.getSourceType() != AnnotationSource.JAVADOC) {
          context.reportError(tn.getTokenStartIndex(), "Using Javadoc syntax in the wrong place");
          return;
        }
        */
        for(int i=0; i<tn.getChildCount(); i++) {
          reportAAST(context, tn.getChild(i));
        }
      } else {
        reportAAST(context, tn);
      }
    } catch (RecognitionException e) {
      handleRecognitionException(context, contents, e);
      return ParseResult.FAIL;
    } catch (Exception e) {
      context.reportException(IAnnotationParsingContext.UNKNOWN, e);
      return ParseResult.FAIL;
    }
    return ParseResult.OK;
  }

  /**
   * Assumes that parsed text specifies where the annotations should go
   */
  private void reportAAST(IAnnotationParsingContext context, Tree tn) {
    AnnotationLocation loc = translateTokenType(tn.getType(), context.getOp());
    final int offset       = context.mapToSource(tn.getTokenStartIndex());        
    try {
      IAASTRootNode d = makeAAST(offset);
      context.reportAAST(offset, loc, tn.getText(), d);
    } catch (Exception e) {
      context.reportException(offset, e);
    }
  }

  protected AnnotationLocation translateTokenType(int type, Operator op) {
    return AnnotationLocation.translateTokenType(type);
  }
  
  /**
   * Calls the appropriate method on the parser
   * @return the created tree 
   */
  protected abstract Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws Exception, RecognitionException;  
}
