/*$Header: 
 * /cvs/fluid/fluid/src/com/surelogic/analysis/IIRAnalysis.java,v 1.4 2008/08/14 20:31:20 chance Exp $*/
package com.surelogic.analysis;

/**
 * The sequence of events for projects P,
 * analyses on comp units Ac, and
 * flow analyses on types and methods At, Am with shared analyses S:
 * 
 *    for (A) {
 *      a.init(env)
 *    }
 *    for (P) {
 *      for (Ai in {Ac, At, Am}) {
 *        for (Ai) {
 *          a.analyzeBegin(env, p)
 *        }                
 *        for (granule : a.getGranules(p)) {
 *          for (Ai) {
 *            a.doAnalysisOnGranule(granule)
 *          }
 *        }        
 *        for (Ai) {
 *          a.analyzeEnd(p)
 *        }
 *        for (Ai) {
 *          a.postAnalysis(p)
 *        }
 *      }
 *    }
 *    for (A) {
 *      a.finish(env)
 *    }
 * 
 * @author Edwin
 */
public interface IIRAnalysis<Q extends IAnalysisGranule> {	
	/**
	 * Used internally for debug output
	 */
	String name();
	boolean analyzeAll();
	
	@Deprecated
	ConcurrencyType runInParallel();
	
	Class<?> getGroup();
	
	/**
	 * Determines how comp units get broken up into analysis granules
	 * If null, CUDrops are used as is
	 */
	IAnalysisGranulator<Q> getGranulator();
	
	/**
	 * Called when the analysis is created, and before any loading
	 */
	void init(IIRAnalysisEnvironment env);

	/**
	 * Called before starting this analysis on the given project
	 */
	void analyzeBegin(IIRAnalysisEnvironment env, IIRProject p);

	/**
	 * May be interleaved with other analyses in its analysis group
	 */
	boolean doAnalysisOnGranule(IIRAnalysisEnvironment env, Q granule);
	
	/**
	 * Called when there is no more (known) work to do for this analysis
	 * 
	 * OK to finish things that don't affect any other analyses
	 */
	Iterable<Q> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p);

	/**
	 * Called after all analysis in its group are done
	 * 
	 * OK to finish things that might have affected other analyses in its group
	 */
	void postAnalysis(IIRProject p);
	
	/**
	 * Called after all analyses are done
	 * 
	 * OK to finish things that might have affected other analyses
	 */
	void finish(IIRAnalysisEnvironment env);
}
