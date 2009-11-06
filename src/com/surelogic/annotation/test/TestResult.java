/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/test/TestResult.java,v 1.22 2009/01/13 21:45:22 aarong Exp $*/
package com.surelogic.annotation.test;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.test.ITest;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.IDerivedDropCreator;

public class TestResult implements ITest {
  public final IRNode node;
  public final TestResultType type;
  public final String context;
  public final String explanation;
  private String promise;
  private IAASTRootNode ast;
  private PromiseDrop<? extends IAASTRootNode> drop;
  
  private TestResult(IRNode n, TestResultType t, String c, String explan) {
    if (n == null || t == null) {
      throw new IllegalArgumentException();
    }
    node    = n;
    type    = t;
    context = c;  
    explanation = explan;
  }
  
  public static TestResult newResult(IRNode n, TestResultType type, String context, String explan) {
    TestResult r = new TestResult(n, type, context, explan);
    AnnotationRules.XML_LOG.reportStart(r);
    return r;
  }
  
  public static TestResult newResult(IRNode n, TestResultType type, String explan) {
    return newResult(n, type, null, explan);
  }
  
  public IRNode getNode() {
    return node;
  }
  
  public String getClassName() {
    IRNode decl    = VisitUtil.getClosestDecl(node);
    return JavaNames.getFullName(decl);
  }
  
  @Override
  public String toString() {
    if (explanation != null) {
      return context == null ? type+" : "+explanation : type+" & "+context+" : "+explanation; 
    }
    return context == null ? type.toString() : type+" & "+context; 
  }
  
  public TestResult cloneResult() {
    TestResult r =  newResult(node, type, context);
    r.promise = promise;
    return r;
  }
  
  /**
   * Check if it's ok to get this far
   * 
   * @return true if ok, false if bad
   */
  private boolean matchesProgress(TestResultType soFar) {
    return soFar.value <= type.value;
  }
  /**
   * Check if we ended up at the right place 
   * 
   * @return true if ok, false if bad
   */
  private boolean matchesResult(TestResultType end) {
    return type == end;
  }
  
  public static void checkIfMatchesResult(TestResult result, TestResultType end) {
    if (result == null) {
      return;
    }
    if (result.matchesResult(end)) {
      AnnotationRules.XML_LOG.reportSuccess(result, "@"+result.promise+" matched "+result); 
    } else {
      AnnotationRules.XML_LOG.reportFailure(result, "@"+result.promise+" is "+end+"; expected "+result);      
    } 
  }
  
  public static void setPromise(TestResult result, String name, String rest) {
    if (result == null) {
      return;
    }
    result.promise  = name+' '+rest;
  }

  public static void addAAST(TestResult result, IAASTRootNode n) {
    if (result == null) {
      return;
    }
    if (!result.matchesProgress(TestResultType.PARSED)) {
      AnnotationRules.XML_LOG.reportFailure(result, "Should NOT have parsed: "+result.promise);
    }
    result.ast = n;
  }
  
  public static void setAsBound(TestResult result) {
    if (result == null) {
      return;
    }
    if (!result.matchesProgress(TestResultType.BOUND)) {
      AnnotationRules.XML_LOG.reportFailure(result, "Should NOT have parsed: "+result.promise);
    }
  }
  
  public static void addDrop(TestResult result, PromiseDrop<? extends IAASTRootNode> d) {
    if (result == null) {
      return;
    }
    IAASTRootNode n = d.getAST();
    if (n != null && n != result.ast) {
      AnnotationRules.XML_LOG.reportFailure(result, "ASTs don't match: "+result.ast+" and "+n);
    }
    if (result.matchesProgress(TestResultType.VALID)) {
      if (result.type == TestResultType.VALID) {
        AnnotationRules.XML_LOG.reportSuccess(result, "@"+result.promise+" matched "+result); 
      }
    } else {
      AnnotationRules.XML_LOG.reportFailure(result, "Should NOT be valid: "+d.getMessage());
    }
    result.drop = d;
  }
  
  public static void checkConsistency() {
    for(Object o : AnnotationRules.XML_LOG.getUnreported()) {
      TestResult result = (TestResult) o; 
      if (result.drop == null) {
        AnnotationRules.XML_LOG.reportFailure(result, "No drop associated with "+result.promise);
        continue; 
      }
      else if (!result.drop.isValid()) {
        AnnotationRules.XML_LOG.reportFailure(result, "Invalid drop associated with "+result.promise);
        continue; 
      }
      else if (result.drop instanceof IDerivedDropCreator) {
    	System.err.println("Ignoring IDerivedDropCreator: "+result.promise);
    	continue;
      }
      TestResultType actual = result.drop.provedConsistent() ? TestResultType.CONSISTENT : TestResultType.INCONSISTENT;
      checkIfMatchesResult(result, actual);
    }
    
//    AnnotationRules.XML_LOG.close();
  }
}
