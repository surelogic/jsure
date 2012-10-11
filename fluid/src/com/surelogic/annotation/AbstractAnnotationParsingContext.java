/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/AbstractAnnotationParsingContext.java,v 1.17 2007/10/22 20:14:06 chance Exp $*/
package com.surelogic.annotation;


import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.test.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * Contains code common to parsing contexts
 * @author Edwin.Chan
 */
public abstract class AbstractAnnotationParsingContext implements
ITestAnnotationParsingContext {
  private final AnnotationSource srcType;
  protected boolean hadProblem = false;
  protected boolean createdAAST = false;
  
  protected AbstractAnnotationParsingContext(AnnotationSource src) {
    srcType = src;
  }
  
  public int getModifiers() {
	  return JavaNode.ALL_FALSE;
  }
  
  public String getProperty(String key) {
	  return null;
  }
  
  public final AnnotationSource getSourceType() {
    return srcType;
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
  
  public void setTestResultForUpcomingPromise(TestResultType r, String explan) {
    setTestResultForUpcomingPromise(TestResult.newResult(getAnnoNode(), r, explan));    
  } 
  
  public void setTestResultForUpcomingPromise(TestResultType r, String context, String explan) {
    setTestResultForUpcomingPromise(TestResult.newResult(getAnnoNode(), r, context, explan));    
  }
  
  /**
   * Returns a default mapping from the offsets seen by the parse rule
   * and the actual source offsets
   */
  public int mapToSource(int offset) {
    return offset;
  }
  
  public String getSelectedText(int start, int stop) {
	throw new UnsupportedOperationException();
  }
    
  public String getAllText() {
	throw new UnsupportedOperationException();
  }
  
  /**
   * Convenience method for annotations that will appear on the default declaration
   */
  public final <T extends IAASTRootNode> void reportAAST(int offset, T ast) {
    reportAAST(offset, AnnotationLocation.DECL, null, ast);
  }
  
  /**
   * Convenience method for AnnotationLocations that don't provide any context
   */
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
