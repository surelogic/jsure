package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

public final class QueryTransformer {
  private static final IRNode[] EMPTY = new IRNode[0];
  private final IRNode[] callers;
  
  private QueryTransformer(final IRNode[] callers) {
    this.callers = callers;
  }
  
  public static QueryTransformer get() {
    return new QueryTransformer(EMPTY);
  }
  
  public QueryTransformer addCaller(final IRNode caller) {
    final IRNode[] newCallers = new IRNode[callers.length + 1];
    System.arraycopy(callers, 0, newCallers, 0, callers.length);
    newCallers[callers.length] = caller;
    return new QueryTransformer(newCallers);
  }
  
  @SuppressWarnings("unchecked")
  public <Q extends HasSubQuery> Q transform(final Q query) {
    HasSubQuery current = query;
    for (final IRNode caller : callers) {
      current = current.getSubAnalysisQuery(caller);
    }
    return (Q) current;
  }
}