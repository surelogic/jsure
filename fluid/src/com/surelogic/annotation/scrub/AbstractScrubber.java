package com.surelogic.annotation.scrub;

import java.util.Comparator;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;

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
	
	protected static final Comparator<IAASTRootNode> aastComparator = new Comparator<IAASTRootNode>() {
		public int compare(IAASTRootNode o1, IAASTRootNode o2) {
			final IRNode p1 = o1.getPromisedFor();
			final IRNode p2 = o2.getPromisedFor();
			int rv;
			if (p1.equals(p2)) {
				// Actually not sufficient, even though we're only comparing things in the same type				
				rv = o1.getOffset() - o2.getOffset();
			} else {
				rv = p1.hashCode() - p2.hashCode();
			}
			if (rv != 0) {
				return rv;
			}
			// Most likely used for library annotations 
			return o1.unparse(false).compareTo(o2.unparse(false));
		}
	};
	
	protected static final Comparator<PromiseDrop<? extends IAASTRootNode>> dropComparator =  
		new Comparator<PromiseDrop<? extends IAASTRootNode>>() {
			public int compare(PromiseDrop<? extends IAASTRootNode> o1,
					PromiseDrop<? extends IAASTRootNode> o2) {
				return aastComparator.compare(o1.getAST(), o2.getAST());
			}
	
	};
}
