/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/DefaultBooleanAnnotationParseRule.java,v 1.13 2008/11/17 18:22:17 chance Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.AASTAdaptor;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.parse.SLParse;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaNode;
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
      Object result = parse(context, SLParse.prototype.initParser(contents));
      AASTAdaptor.Node tn    = (AASTAdaptor.Node) result;
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
    final int offset       = /*context.mapToSource(*/tn.getTokenStartIndex(); //);        
    try {
      AASTAdaptor.Node node = (AASTAdaptor.Node) tn;
      int mods;
      if (node.getModifiers() != JavaNode.ALL_FALSE) {
    	  mods = node.getModifiers();
      } else {
    	  mods = context.getModifiers();
      }
      IAASTRootNode d = makeAAST(context, offset, mods);
      final Object o;
      if (loc == AnnotationLocation.QUALIFIED_RECEIVER) {
    	  Tree child0 = node.getChild(0);
    	  o = child0.getText();
      } else {
    	  o = tn.getText();
      }
      context.reportAAST(offset, loc, o, d);
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
