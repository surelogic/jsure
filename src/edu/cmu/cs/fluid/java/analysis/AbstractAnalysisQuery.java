package edu.cmu.cs.fluid.java.analysis;

import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

public abstract class AbstractAnalysisQuery<R, T, L extends Lattice<T>, A extends FlowAnalysis<T, L>>
    implements AnalysisQuery<R> {
  protected final A analysis;
  
  protected AbstractAnalysisQuery(final A a) {
    analysis = a;    
  }
//  
//  public final boolean hasSubanalysisQuery() {
//    return analysis.getSubAnalysis() != null;
//  }
//
//  protected final void checkSubanalysis() {
//    if (analysis.getSubAnalysis() == null) {
//      throw new UnsupportedOperationException(
//          "Query's analysis does not have a subanalysis");
//    }
//  }
}
