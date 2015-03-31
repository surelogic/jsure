/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/AbstractAnnotationParsingContext.java,v 1.17 2007/10/22 20:14:06 chance Exp $*/
package com.surelogic.annotation;


import java.lang.annotation.Annotation;

import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.test.*;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Contains code common to parsing contexts
 * @author Edwin.Chan
 */
public abstract class AbstractAnnotationParsingContext implements
ITestAnnotationParsingContext {
  private final AnnotationSource srcType;
  private final AnnotationOrigin origin;
  protected boolean hadProblem = false;
  protected boolean createdAAST = false;
  
  protected AbstractAnnotationParsingContext(AnnotationSource src, AnnotationOrigin o) {
    srcType = src;
    origin = o;
  }
  
  @Override
  public int getModifiers() {
	  return JavaNode.ALL_FALSE;
  }
  
  @Override
  public String getProperty(String key) {
	  return null;
  }
  
  @Override
  public void setProperty(String key, String value) {
	  // ignore
  }
  
  @Override
  public final AnnotationSource getSourceType() {
    return srcType;
  }
    
  public final AnnotationOrigin getOrigin() {
    return origin;
  }
  
  public boolean createdAAST() {
    return createdAAST;
  }

  public boolean hadProblem() {
    return hadProblem;
  }
  
  /**
   * e.g. the annotation being checked for test results
   */
  protected abstract IRNode getAnnoNode();
  
  public TestResult getTestResult() {
    throw new UnsupportedOperationException();
  }
  
  public void setTestResultForUpcomingPromise(TestResult r) {
    throw new UnsupportedOperationException();
  }
  
  public void clearTestResult() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void setTestResultForUpcomingPromise(TestResultType r, String explan) {
    setTestResultForUpcomingPromise(TestResult.newResult(getAnnoNode(), r, explan));    
  } 
  
  @Override
  public void setTestResultForUpcomingPromise(TestResultType r, String context, String explan) {
    setTestResultForUpcomingPromise(TestResult.newResult(getAnnoNode(), r, context, explan));    
  }
  
  /**
   * Returns a default mapping from the offsets seen by the parse rule
   * and the actual source offsets
   */
  @Override
  public int mapToSource(int offset) {
    return offset;
  }
  
  @Override
  public String getSelectedText(int start, int stop) {
	throw new UnsupportedOperationException();
  }
    
  @Override
  public String getAllText() {
	throw new UnsupportedOperationException();
  }
  
  /**
   * Convenience method for annotations that will appear on the default declaration
   */
  @Override
  public final <T extends IAASTRootNode> void reportAAST(int offset, T ast) {
    reportAAST(offset, AnnotationLocation.DECL, null, ast);
  }
  
  @Override
  public final void reportError(int offset, String msg) {
	reportErrorAndProposal(offset, msg, null);
  }
 
  @Override
  public void reportError(int offset, int number, Object... args) {
	reportErrorAndProposal(offset, I18N.res(number, args), null);
  }
  
  @Override
  public ProposedPromiseDrop.Builder startProposal(Class<? extends Annotation> anno) {
	  return null;
  }
  
  /**
   * Convenience method for AnnotationLocations that don't provide any context
   */
  @Override
  public final <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, T ast) {
    reportAAST(offset, loc, null, ast);
  }
  
  /**
   * Finds the nearest enclosing declaration from the
   * given node
   */
  protected final IRNode computeDeclNode(IRNode node) {
    final IRNode start = node;
    while (node != null) {
      Operator op = JJNode.tree.getOperator(node);
      if (Declaration.prototype.includes(op)) {
        return node;
      } 
      else if (VariableDeclList.prototype.includes(op)) {
        return node;
      }
      else if (MethodCall.prototype.includes(op)) {
    	return node; // Special case for Cast
      }
      else if (BlockStatement.prototype.includes(op)) {
   	    return node;
      }      
      node = JJNode.tree.getParentOrNull(node);
    }
    return start;
  }
  
  /**
   * Finds the appropriate declaration to annotate, based on the parameters
   * 
   * @param decl The base location to start from
   * @param loc  The relative location
   * @param context Context info used to find the appropriate declaration
   */
  private IRNode translateDecl(IRNode decl, AnnotationLocation loc, Object context) {
    switch (loc) {
      case DECL:
        return decl;
      case RECEIVER:
        return JavaPromise.getReceiverNodeOrNull(decl);
      case RETURN_VAL:
        return JavaPromise.getReturnNodeOrNull(decl);
      case PARAMETER:
        String id = (String) context;
        return BindUtil.findLV(decl, id);
      case QUALIFIED_RECEIVER:    	    	
    	IRNode type = (IRNode) context;
    	return JavaPromise.getQualifiedReceiverNodeByName(decl, type);
    }
    return null;
  }
  
  /**
   * Helper method to compute the declarations to annotate from the IRNode where
   * the text of annotation appears
   */
  protected final Iterable<IRNode> computeDeclNode(IRNode node, AnnotationLocation loc, Object o) {
    IRNode decl = computeDeclNode(node);
    if (VariableDeclList.prototype.includes(decl)) {
      IRNode vds = VariableDeclList.getVars(decl);
      return VariableDeclarators.getVarIterator(vds);
    } else {
      return new SingletonIterator<IRNode>(translateDecl(decl, loc, o));
    }
  }
}