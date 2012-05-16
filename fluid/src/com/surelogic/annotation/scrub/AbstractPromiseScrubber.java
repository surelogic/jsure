package com.surelogic.annotation.scrub;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Works like AbstractAASTScrubber, but on PromiseDrops.
 * Requires you to define getRelevantAnnotations() and override other methods if needed
 * 
 * Probably registered as shown below:
 * 
 *  @Override
 *  public void register(PromiseFramework fw) {
 *    ...
 *    registerScrubber(fw, myNewScrubber);
 *  } 
 * @author Edwin
 */
public abstract class AbstractPromiseScrubber<P extends PromiseDrop<? extends IAASTRootNode>> extends AbstractHierarchyScrubber<P> {
	protected AbstractPromiseScrubber(ScrubberType type, String[] before,
			String name, ScrubberOrder order, String[] deps) {
		super(type, before, name, order, deps);
	}

	protected abstract void processDrop(P a);
	
	@Override
	protected void processAASTsForType(IAnnotationTraversalCallback<P> ignored,
			IRNode decl, List<P> l) {
		try {
			// Sort to process in a consistent order
			Collections.sort(l, dropComparator);
		} catch(IllegalArgumentException e) {
			SLLogger.getLogger().log(Level.WARNING, "While sorting AASTs for "+JavaNames.getFullName(decl), e);
		}
		
		for(P a : l) {
	    /*
		if ("MUTEX".equals(a.toString())) {
			System.out.println("Scrubbing: "+a.toString());						
		}
        */
			processDrop(a);
		}		
	}

	private final IAnnotationTraversalCallback<P> nullCallback = new IAnnotationTraversalCallback<P>() {
		public void addDerived(P pd, PromiseDrop<? extends P> nonsensical) {
			getContext().reportWarning("Ignoring derived drop "+pd, pd.getAST());
		}
	};
	
	@Override
	protected IAnnotationTraversalCallback<P> getNullCallback() {
		return nullCallback;
	}
	
	@Override
	protected void finishAddDerived(P clone, PromiseDrop<? extends P> nonsensical) {
		// Nothing to do?
	}

	@Override
	protected void finishRun() {
		// Nothing to do?
		// TODO Reset state?
	}
}
