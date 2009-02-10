package com.surelogic.jsure.views.debug.testResults.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.test.ITest;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class Root {
  private final Map<IRNode, Heading> headers;
  private final Map<Heading, Set<AbstractTestResult>> testResults;
  
  
  
  public Root() {
    headers = new HashMap<IRNode, Heading>();
    testResults = new HashMap<Heading, Set<AbstractTestResult>>();
  }
  
  
  
  public void addSuccess(final ITest test, final String message) {
    addTestResult(new SuccessfulTestResult(test, message));
  }
  
  public void addFailure(final ITest test, final String message) {
    addTestResult(new FailedTestResult(test, message)).setFailed();
  }
  
  public void addError(final ITest test, final Throwable error) {
    addTestResult(new ExceptionalTestResult(test, error)).setFailed();
  }
  
  public Heading computeParent(final AbstractTestResult testResult) {
    final IRNode node = VisitUtil.getClosestDecl(testResult.getNode());
    Heading heading = headers.get(node);
    if (heading == null) {
      heading = new Heading(node);
      headers.put(node, heading);
    }
    return heading;
  }
  
  private Heading addTestResult(final AbstractTestResult testResult) {
    final Heading parent = computeParent(testResult);
    Set<AbstractTestResult> results = testResults.get(parent);
    if (results == null) {
      results = new HashSet<AbstractTestResult>();
      testResults.put(parent, results);
    }
    results.add(testResult);
    return parent;
  }
  
  public void clear() {
    headers.clear();
    testResults.clear();
  }
  
  public Object[] getHeadings() {
    return testResults.keySet().toArray();
  }
  
  public Object[] getChildren(final Heading heading) {
    return testResults.get(heading).toArray();
  }
}
