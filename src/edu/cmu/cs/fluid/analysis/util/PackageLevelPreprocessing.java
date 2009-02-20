package edu.cmu.cs.fluid.analysis.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.eclipse.adapter.ModuleUtil;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;

/**
 * Analysis module to preprocess package-level constructs, in particular, scoped
 * 
 * @module declarations, so that they can be used to determine which CUs get
 *         loaded.
 */
public final class PackageLevelPreprocessing extends
		AbstractFluidAnalysisModule {
	private static PackageLevelPreprocessing INSTANCE;

	/**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
	public static PackageLevelPreprocessing getInstance() {
		return INSTANCE;
	}

	/**
	 * Public constructor that will be called by Eclipse when this analysis
	 * module is created.
	 */
	public PackageLevelPreprocessing() {
		INSTANCE = this;
		Eclipse.initialize();
	}

	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger
			.getLogger("analysis.util.PackageLevelPreprocessing");

	@Override
	public void preBuild(IProject p) {
		ModuleUtil.clearModuleCache();
		super.preBuild(p);
	}

	@Override
	public void analyzeBegin(IProject p) {
		super.analyzeBegin(p);
		IDE.getInstance().setAdapting();
	}

	/**
	 * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeResource(org.eclipse.core.resources.IResource,
	 *      int)
	 */
	@Override
	public boolean analyzeResource(final IResource resource, final int kind) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				if (AbstractFluidAnalysisModule.isPackageInfo(resource)) {
					switch (kind) {
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED:
						final boolean pkgAlreadyLoaded = true; // FIX
																// Binding.packageExists(name);
						// conservatively reload package, since promises need to
						// be added
						// Also creates matchers for scoped promises as a
						// by-product
						PackageDrop p = PromiseParser.getInstance()
								.parsePackageInfo(resource);
						if (p == null) {
							if (LOG.isLoggable(Level.FINE))
								LOG.fine("package-info.java not on classpath");
						} else if (pkgAlreadyLoaded) {
							queueForLaterProcessing(p);
						}
						break;
					case IResourceDelta.REMOVED:
						throw new UnsupportedOperationException(
								"conservatively reload package, since promises need to be deleted");
					default:
						LOG.severe("Not handling removal of "
								+ resource.getName());
					}
				} else if (AbstractFluidAnalysisModule.isPromisesXML(resource)) {
					switch (kind) {
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED:
						final String qname = getCorrespondingPackageName(resource);
						if (qname != null) {
							final boolean pkgExists = Binding
									.packageExists(qname);
							final boolean pkgAlreadyLoaded;
							if (pkgExists) {
								PackageDrop oldPkgDrop = PackageDrop
										.findPackage(qname);
								if (oldPkgDrop != null) {
									pkgAlreadyLoaded = true;
									oldPkgDrop.invalidate();
								} else {
									pkgAlreadyLoaded = false;
								}
							} else {
								pkgAlreadyLoaded = false;
							}

							// conservatively reload package, since promises
							// need to be added
							// Also creates matchers for scoped promises as a
							// by-product
							PackageDrop p = Binding.confirmPackage(qname);
							if (pkgAlreadyLoaded) {
								queueForLaterProcessing(p);
							}
						}
						break;
					case IResourceDelta.REMOVED:
						throw new UnsupportedOperationException(
								"conservatively reload package, since promises need to be deleted");
					default:
						LOG.severe("Not handling removal of "
								+ resource.getName());
					}
				}
			}
		});
		// No need for analyzeCompilationUnit to be called
		return true;
	}

	/**
	 * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeCompilationUnit(org.eclipse.jdt.core.ICompilationUnit,
	 *      org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public void analyzeCompilationUnit(final ICompilationUnit file,
			CompilationUnit ast) {
		LOG.severe("analyzeCompilationUnit() called on "
				+ file.getElementName());
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IResource[] analyzeEnd(IProject p) {
		IResource[] results = super.analyzeEnd(p);
		IDE.getInstance().clearAdapting();
		return results;
	}
}
