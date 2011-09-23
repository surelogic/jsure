package com.surelogic.annotation.scrub;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;

public abstract class AbstractScrubber implements IAnnotationScrubber {
	private IAnnotationScrubberContext context;
	private final String name;
	private final String[] dependencies;
	private final String[] runsBefore;
	private final ScrubberOrder order;

	public AbstractScrubber(String[] before, String name, ScrubberOrder order, String... deps) {
		this.name         = name;
		this.order        = order;
		this.dependencies = deps;    
		this.runsBefore   = before;
	}

	/**
	 * Returns the scrubber's name
	 */
	public final String name() {
		return name;
	}

	public ScrubberOrder order() {
		return order;
	}

	/**
	 * Returns a list of strings, each of which is the name of another scrubber
	 * that this scrubber depends on having run before it.
	 */
	public final String[] dependsOn() {
		return dependencies;
	}

	/**
	 * Returns a list of strings, each of which is the name of another scrubber
	 * that this scrubber needs to run before.
	 */
	public final String[] shouldRunBefore() {
		return runsBefore;
	}

	/**
	 * Sets the context for reporting errors, etc.
	 */
	public final void setContext(IAnnotationScrubberContext c) {
		context = c;
	}
		  
	/**
	 * Returns the context
	 */
	protected final IAnnotationScrubberContext getContext() {
		return context;
	}
	 
	protected static final void markAsUnassociated(IAASTRootNode a) {
		TestResult expected = AASTStore.getTestResult(a);   
		TestResult.checkIfMatchesResult(expected, TestResultType.UNASSOCIATED);
	}
}
