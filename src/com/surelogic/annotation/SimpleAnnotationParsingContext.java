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
  
  @Override
  public int mapToSource(int offset) {
    return this.offset;// + offset;
  }
  
  @Override
  public String getSelectedText(int start, int stop) {
	return contents.substring(start, stop);
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
        reportError(offset, "Couldn't find "+o);
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
  
  public void reportError(int offset, String msg) {
    String txt = getName()+":"+offset+" -- "+msg;
//    System.out.println(txt);
    
    TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
    
    PromiseWarningDrop d = new PromiseWarningDrop(mapToSource(offset));
    d.setMessage(txt);
    d.setCategory(JavaGlobals.PROMISE_PARSER_PROBLEM);
    d.setNodeAndCompilationUnitDependency(node);
    hadProblem = true;
  }

  public void reportException(int offset, Exception e) {
    TestResult.checkIfMatchesResult(getTestResult(), TestResultType.UNPARSEABLE);
    
    PromiseWarningDrop d = new PromiseWarningDrop(mapToSource(offset));
    d.setCategory(JavaGlobals.PROMISE_PARSER_PROBLEM);
    d.setNodeAndCompilationUnitDependency(node);
    
    if (e instanceof RecognitionException ||
        e instanceof RewriteCardinalityException) {
      String txt = e.toString();
      LOG.warning(txt);
      d.setMessage(txt);
    } else {
      LOG.log(Level.SEVERE, "Unexpected problem while parsing promise", e);
    }
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
