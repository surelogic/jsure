/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Abstraction of a query to an analysis.  Implementations of
 * {@link IntraproceduralAnalysis} have getter methods that return instances
 * of these.  This class is primarily intended to abstract away the 
 * constructor context parameter.
 */
public interface AnalysisQuery<T> {
  public T getResultFor(IRNode expr);
}
