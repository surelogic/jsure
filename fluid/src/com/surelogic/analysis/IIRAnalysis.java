/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/IIRAnalysis.java,v 1.4 2008/08/14 20:31:20 chance Exp $*/
package com.surelogic.analysis;

import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * The sequence of events for analyses A1 to An, projects P1, P2 with shared analyses S1, S2:
 *    for (A) {
 *      a.init(env)
 *    }
 *    for (A) {
 *      for (P) {
 *        for (S) {
 *          s.analyzeBegin(env, p)
 *        }
 *        for (p) {
 *          s.doAnalysisOnAFile(...)
 *        }        
 *        for (S) {
 *          s.analyzeEnd(p)
 *        }
 *        for (S) {
 *          s.postAnalysis(p)
 *        }
 *      }
 *    }
 *    for (A) {
 *      a.finish(env)
 *    }
 * 
 * @author Edwin
 */
public interface IIRAnalysis {	
	/**
	 * Used internally for debug output
	 */
	String name();
	boolean analyzeAll();
	ConcurrencyType runInParallel();
	Class<?> getGroup();
	
	/**
	 * Called when the analysis is created, and before any loading
	 */
	void init(IIRAnalysisEnvironment env);
	
	/**
	 * Called after ASTs are ready, but before any analysis
	 */
	//void preAnalysis(IIRAnalysisEnvironment env);

	/**
	 * Called before starting this analysis on the given project
	 */
	void analyzeBegin(IIRAnalysisEnvironment env, IIRProject p);

	/**
	 * May be interleaved with other analyses in its analysis group
	 */
	boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud);
	
	/**
	 * Called when there is no more (known) work to do for this analysis
	 * 
	 * OK to finish things that don't affect any other analyses
	 */
	Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p);

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
