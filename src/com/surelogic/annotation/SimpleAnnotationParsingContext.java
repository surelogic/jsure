/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/SimpleAnnotationParsingContext.java,v 1.5 2008/06/24 19:13:15 thallora Exp $*/
package com.surelogic.annotation;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.RewriteCardinalityException;

import com.surelogic.aast.*;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class SimpleAnnotationParsingContext extends AbstractAnnotationParsingContext {
  static final Logger LOG = SLLogger.getLogger("sl.annotation");
  
  final IRNode node;
  final IAnnotationParseRule<?,?> rule;
  final String contents;
  final int offset;
  
  protected SimpleAnnotationParsingContext(AnnotationSource src, IRNode n, 
                                           IAnnotationParseRule<?,?> r, String text, int offset) {    
    super(src);
    node = n;
    rule = r;
    contents = text;  
    this.offset = offset;
  }
  @Override
  protected final IRNode getNode() {
    return node;
  }
  
  public IAnnotationParseRule<?,?> getRule() {
	  return rule;
  }
  
  @Override
  public int mapToSource(int offset) {
    return this.offset;// + offset;
  }
  
  @Override
  public String getSelectedText(int start, int stop) {
	return contents.substring(start, stop);
  }
  
  @Override
  public String getAllText() {
	return contents;
  }
  
  protected abstract String getName();
  
  /**
   * Abstract method that is called after an AAST is created
   * @param root The AASTRootNode that was created
   */
  protected abstract void postAASTCreate(final AASTRootNode root);
  
  public <T extends IAASTRootNode> 
  void reportAAST(int offset, AnnotationLocation loc, Object o, T ast) {
    if (ast == null) {
      throw new IllegalArgumentException("Null ast");
    }
    
    // In some cases, we need the results of the parse to tell us 
    // what this node should be
    TestResult result = getTestResult();     
    boolean first = true;
    for(IRNode declNode : computeDeclNode(node, loc, o)) {
      AASTRootNode root;
      TestResult tr;
      
      if (first) {
        first = false;
        root  = (AASTRootNode) ast;
        tr    = result;
      } else {
        root  = (AASTRootNode) ast.cloneTree();      
        tr    = result == null ? null : result.cloneResult();
      }
      root.setSrcType(getSourceType());
      // CHANGED from pointing at the promise node
      // FIX drop.setNodeAndCompilationUnitDependency(declNode); 
      if (declNode == null) {
    	final IRNode decl = VisitUtil.getEnclosingDecl(node);
    	final Operator op = JJNode.tree.getOperator(decl);
    	final boolean isFunction = SomeFunctionDeclaration.prototype.includes(op);
    	final String msg;
    	switch (loc) {
    	case RECEIVER:
    		if (isFunction) {
    			if (TypeUtil.isStatic(decl)) {
    				msg = "Cannot use @"+rule.name()+"("+o+") on a static method, since it does not have a receiver";
    				break;
    			}
    		}
    		msg = "No receiver on this "+getUnit(op)+" to annotate with @"+rule.name();    		
			break;
    	case RETURN_VAL:    
    		if (isFunction && MethodDeclaration.prototype.includes(op)) {
    			IRNode rtype = MethodDeclaration.getReturnType(decl);
    			if (VoidType.prototype.includes(rtype)) {
    				msg = "Cannot use @"+rule.name()+"("+o+") on a void method, since it does not have a return value";
    				break;
    			}
    		}
    		msg = "No return value on this "+getUnit(op)+" to annotate with @"+rule.name();   
    		break;
    	default:
    		msg = "Couldn't find '"+o+"' to annotate with @"+rule.name();
    	}
    	reportError(offset, msg);
        root.markAsUnbound();      
      } else {        
        root.setPromisedFor(declNode);
        
        //final PromiseFramework pw = PromiseFramework.getInstance();
        //pw.addSlotValue(rule, declNode, drop);
        TestResult.addAAST(tr, root);
         
        AASTStore.add(root);
        AASTStore.associateTestResult(root, tr);
        createdAAST = true;
        postAASTCreate(root);
      }
    }
    clearTestResult();
  }  
  
  public static String getUnit(Operator op) {
	  if (ConstructorDeclaration.prototype.includes(op)) {
		  return "constructor";
	  }
	  else if (MethodDeclaration.prototype.includes(op)) {
		  return "method";
	  }
	  else if (FieldDeclaration.prototype.includes(op)) {
		  return "field";
	  }
	  else if (TypeDeclaration.prototype.includes(op)) {
		  return "type";		  
	  }	  
	  return "declaration";
  }
  
  public static void reportError(IRNode node, int offset, String txt) {	    
	  PromiseWarningDrop d = new PromiseWarningDrop(offset);
	  d.setMessage(txt);
	  d.setCategory(JavaGlobals.PROMISE_PARSER_PROBLEM);
	  d.setNodeAndCompilationUnitDependency(node);
  }
  
  public void reportError(int offset, String msg) {
    TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
    
	final int position = mapToSource(offset);
    String txt = getName()+":"+offset+" -- "+msg;
    reportError(node, position, txt);
    hadProblem = true;
  }

  public void reportException(int offset, Exception e) {
    TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
    
	final int position = mapToSource(offset);
	final String txt;
	if (e instanceof RecognitionException ||
	    e instanceof RewriteCardinalityException) {
		txt = e.toString();
		LOG.warning(txt);
	} else {
		LOG.log(Level.SEVERE, "Unexpected problem while parsing promise", e);
		txt = "Unexpected problem while parsing promise: "+e.getMessage();
	}
	reportError(node, position, txt);


    hadProblem = true;
  }

  public Operator getOp() {
    IRNode decl = computeDeclNode(node);
    return JJNode.tree.getOperator(decl);
  }  
  
  @Override
  public TestResult getTestResult() {
    return null;
  }
  @Override
  public void setTestResultForUpcomingPromise(TestResult r) {
    // nothing to do
  }
  @Override
  public void clearTestResult() {
    // nothing to do
  }
}
