package edu.cmu.cs.fluid.analysis.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.dc.AbstractAnalysisModule;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.adapter.TypeBindings;
import edu.cmu.cs.fluid.ide.IDE;

/**
 * FIX rename to fit what this code now does
 * 
 * Recast as a prepass to converting FAST to IR
 */
public final class UpdateSuperRootStorage extends AbstractFluidAnalysisModule<Void> {

	private static UpdateSuperRootStorage INSTANCE;

	/**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
	public static UpdateSuperRootStorage getInstance() {
		return INSTANCE;
	}

	/**
	 * Public constructor that will be called by Eclipse when this analysis
	 * module is created.
	 */
	public UpdateSuperRootStorage() {
		INSTANCE = this;
		Eclipse.initialize();
	}

	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger
			.getLogger("analysis.util.UpdateSuperRootStorage");

	private long used;

	private int compUnits;
	
	@Override
	public void analyzeBegin(IProject p) {
		super.analyzeBegin(p);
		used = edu.cmu.cs.fluid.dc.Plugin.memoryUsed();
		compUnits = 0;

		// Clear binder cache
		IDE.getInstance().notifyASTsChanged();
	}

	/**
	 * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeResource(org.eclipse.core.resources.IResource,
	 *      int)
	 */
	@Override
	public boolean analyzeResource(IResource resource, int kind) {
		/* TODO update for new processor?
		// FIX how to ignore build path resources?
		if (AbstractFluidAnalysisModule.isPromisesXML(resource) && onLibPath(resource)) {
			if (isOnOutputPath(resource)) {
				return true; // ignore
			}
			IFile xml = (IFile) resource;
			String name = xml.getLocation().toOSString();
			if (isRemoved(kind)) {
				Eclipse.getDefault().getContext(getProject()).getXmlProcessor()
						.unregisterXML(name);
				return true;
			}
			Eclipse.getDefault().getContext(getProject()).getXmlProcessor()
					.registerXML(name);
			return true;
		}
		*/
		// need analyzeCompilationUnit to be called
		return super.analyzeResource(resource, kind);
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#needsAST()
	 */
	@Override
	public boolean needsAST() {
		return false;
	}

	/**
	 * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeCompilationUnit(org.eclipse.jdt.core.ICompilationUnit,
	 *      org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public boolean analyzeCompilationUnit(final ICompilationUnit file,
			CompilationUnit ast, 
            IAnalysisMonitor monitor) {
		compUnits++;
		return false;
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IResource[] analyzeEnd(IProject p, IAnalysisMonitor monitor) {
		long used2 = edu.cmu.cs.fluid.dc.Plugin.memoryUsed();
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("BEFORE memory = " + used);
			LOG.fine("AFTER  memory = " + used2);
			LOG.fine("DELTA  memory = " + (used2 - used));
			LOG.fine("Comp units   = " + compUnits);
			LOG.fine("Type bindings = " + TypeBindings.numTypesBound());
		}
		return AbstractAnalysisModule.NONE_FURTHER;
	}

	@Override
	protected void removeResource(IResource resource) {
		// Nothing to do?
	}
}
