package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.java.bind.IBinder;

/** Keep track of a set of analyses and the binder in one class.
 * We may have more than one instance of this class around at any time.
 */
@Deprecated
public class AnalysisContext {
  public final IBinder binder;
  public final TypeBasedAliasAnalysis tbAlias;
  
  private AnalysisContext(final IBinder b) {
    binder = b;
    tbAlias = new TypeBasedAliasAnalysis(b);
  }
  
  public static AnalysisContext getContext(final IBinder b) {
    // cache?
    return new AnalysisContext(b);
  }
}
