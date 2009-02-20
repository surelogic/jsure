package edu.cmu.cs.fluid.analysis.lock;

import com.surelogic.analysis.locks.*;

import edu.cmu.cs.fluid.analysis.util.*;
import edu.cmu.cs.fluid.dc.IAnalysis;

/**
 * Analysis routine to perform <I>Greenhouse</I> Java lock policy assurance.
 */
public final class LockAssurance3 extends AbstractWholeAnalysisModule {
	private static LockAssurance3 INSTANCE;	

	/**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
	public static IAnalysis getInstance() {
		return INSTANCE;
	}

	/**
	 * Public constructor that will be called by Eclipse when this analysis
	 * module is created.
	 */
	public LockAssurance3() {
		super(new LockAnalysis());
		INSTANCE = this;
	}
}