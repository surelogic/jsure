package edu.cmu.cs.fluid.analysis.util;

import org.eclipse.core.resources.IProject;

import com.surelogic.analysis.IIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

/**
 * Uses an IIRAnalysis to analyze a whole program
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractWholeAnalysisModule extends AbstractWholeIRAnalysisModule {	
	private static IIRAnalysisEnvironment env = new IIRAnalysisEnvironment() {
		public void ensureClassIsLoaded(String qname) {
			ConvertToIR.prefetch(qname);
		}
	};
	
	private final IIRAnalysis analysis;
	
	protected AbstractWholeAnalysisModule(IIRAnalysis a) {
		super(ParserNeed.NEW);
		if (!a.analyzeAll()) {
			throw new IllegalArgumentException("Not a whole-program analysis");
		}
		analysis = a;
		analysis.init(env);
	}
	
	@Override
	public final void preBuild(IProject project) {
		super.preBuild(project);
		analysis.preAnalysis(env, Eclipse.getDefault().makeClassPath(project));
	}
	
	@Override
	public final void analyzeBegin(IProject project) {
		super.analyzeBegin(project);
		analysis.analyzeBegin(Eclipse.getDefault().makeClassPath(project));
	}
	
	@Override
	protected final void doAnalysisOnAFile(final CUDrop drop) {
		analysis.doAnalysisOnAFile(drop);
	}
	
	@Override
	protected final void doAnalysisOnAFile(IRNode cu) {
		throw new UnsupportedOperationException("Requires a CUDrop");
	}
	
	@Override
	public final Iterable<IRNode> finishAnalysis(IProject project) {
		if (doingFullProjectPass) {
			// Ignoring return value, since we're done with the full pass
			analysis.analyzeEnd(Eclipse.getDefault().makeClassPath(project));
		}
		return super.finishAnalysis(project);
	}
	
	@Override
	public final void postBuild(IProject project) {
		analysis.postAnalysis(Eclipse.getDefault().makeClassPath(project));
		super.postBuild(project);
		IDE.getInstance().clearCaches();
	}
}
