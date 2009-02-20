package edu.cmu.cs.fluid.analysis.threadeffects;

import com.surelogic.analysis.threads.*;

import edu.cmu.cs.fluid.analysis.util.*;

public class ThreadEffects2 extends AbstractWholeAnalysisModule {
	public ThreadEffects2() {
		super(new ThreadEffectsModule());
	}
}