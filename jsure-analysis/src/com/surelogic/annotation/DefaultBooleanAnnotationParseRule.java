package com.surelogic.annotation;

import java.lang.reflect.Constructor;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.parse.AASTAdaptor;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.parse.SLParse;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Abstract class for boolean parse rules (e.g. @Unique)
 * 
 * @author Edwin.Chan
 */
public abstract class DefaultBooleanAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<A>>
    extends AbstractAnnotationParseRule<A, P> {
  // private static final Class[] defaultParamTypes = new Class[] { int.class,
  // int.class };

  public static final Class<?>[] noParamTypes = new Class[0];

  protected DefaultBooleanAnnotationParseRule(String name, Operator[] ops, Class<A> dt) {
    super(name, ops, dt);
  }

  protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int mappedOffset, int modifiers, AASTAdaptor.Node node)
      throws Exception {
    return makeAAST(context, mappedOffset, modifiers);
  }

  /**
   * Uses reflection to create an AAST root node of the appropriate type; (kept
   * for compatibility with older code)
   * 
   * @param offset
   *          mapped
   */
  protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int modifiers) throws Exception {
    // Constructor<A> c = getAASTType().getConstructor(defaultParamTypes);
    // return c.newInstance(context.mapToSource(offset), modifiers);
    // context.mapToSource()
    Constructor<A> c = getAASTType().getConstructor(noParamTypes);
    return c.newInstance();
  }

  /**
   * Assumes that parsed text specifies where the annotations should go Handles
   * differences between syntax for Java 5 and Javadoc
   */
  @Override
  public final ParseResult parse(IAnnotationParsingContext context, String contents) {
    if (!declaredOnValidOp(context.getOp())) {
      context.reportError(IAnnotationParsingContext.UNKNOWN, "Promise declared on invalid operator");
      return ParseResult.FAIL;
    }
    try {
      Object result = parse(context, SLParse.prototype.initParser(contents));
      AASTAdaptor.Node tn = (AASTAdaptor.Node) result;
      if (tn == null) {
        return ParseResult.FAIL;
      }
      if (tn.getType() == SLAnnotationsParser.Expressions) {
        /*
         * if (context.getSourceType() != AnnotationSource.JAVADOC) {
         * context.reportError(tn.getTokenStartIndex(),
         * "Using Javadoc syntax in the wrong place"); return; }
         */
        for (int i = 0; i < tn.getChildCount(); i++) {
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
  protected void reportAAST(IAnnotationParsingContext context, Tree tn) {
    AnnotationLocation loc = translateTokenType(tn.getType(), context.getOp());
    final int offset = tn.getTokenStartIndex();
    try {
      AASTAdaptor.Node node = (AASTAdaptor.Node) tn;
      int mods;
      if (node.getModifiers() != JavaNode.ALL_FALSE) {
        mods = node.getModifiers();
      } else {
        mods = context.getModifiers();
      }
      IAASTRootNode d = makeAAST(context, context.mapToSource(offset), mods, node);
      if (d != null) {
        final Object o;
        if (loc == AnnotationLocation.QUALIFIED_RECEIVER) {
          Tree child0 = node.getChild(0);
          o = child0.getText();
        } else {
          o = tn.getText();
        }
        context.reportAAST(offset, loc, o, d);
      }
    } catch (Exception e) {
      context.reportException(offset, e);
    }
  }

  protected AnnotationLocation translateTokenType(int type, Operator op) {
    return AnnotationLocation.translateTokenType(type);
  }

  /**
   * Calls the appropriate method on the parser
   * 
   * @return the created tree
   */
  protected abstract Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser)
      throws Exception, RecognitionException;

  @Override
  public boolean appliesTo(final IRNode decl, final Operator op) {
    // final Operator op = JJNode.tree.getOperator(decl);
    if (ParameterDeclaration.prototype.includes(op)) {
      return ReferenceType.prototype.includes(ParameterDeclaration.getType(decl));
    } else if (FieldDeclaration.prototype.includes(op)) {
      if (!ReferenceType.prototype.includes(FieldDeclaration.getType(decl))) {
        // Check if all arrays
        for (IRNode vd : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(decl))) {
          if (VariableDeclarator.getDims(vd) == 0) {
            return false;
          }
        }
      }
      return true;
    }
    return super.appliesTo(decl, op);
  }

  protected static boolean isRefTypedMethod(IRNode n) {
    return ReferenceType.prototype.includes(MethodDeclaration.getReturnType(n));
  }
}
