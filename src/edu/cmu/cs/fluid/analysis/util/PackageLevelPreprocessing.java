package edu.cmu.cs.fluid.analysis.util;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.PromiseDropStorage;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.EclipseCodeFile;
import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.eclipse.adapter.JavaSourceFileAdapter;
import edu.cmu.cs.fluid.eclipse.adapter.ModuleUtil;
import edu.cmu.cs.fluid.eclipse.adapter.SrcRef;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * Analysis module to preprocess package-level constructs, in particular, scoped
 * 
 * @module declarations, so that they can be used to determine which CUs get
 *         loaded.
 */
public final class PackageLevelPreprocessing extends
		AbstractFluidAnalysisModule<Void> {
	private static PackageLevelPreprocessing INSTANCE;
	private final ASTParser parser = ASTParser.newParser(AST.JLS3);
	private Dependencies dependencies;
	
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
		dependencies = new Dependencies();
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
				PackageDrop p = null;
				if (AbstractFluidAnalysisModule.isPackageInfo(resource)) {
					switch (kind) {
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED:
					case IResourceDelta.REMOVED:						
						p = parsePackageInfo(resource);
						break;
					default:
						LOG.severe("Not handling removal of "
								+ resource.getName());
					}
				} else if (AbstractFluidAnalysisModule.isPromisesXML(resource)) {
					switch (kind) {
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED:
					case IResourceDelta.REMOVED:
						final String qname = getCorrespondingPackageName(resource);
						if (qname != null) {
							final boolean pkgExists = Binding.packageExists(qname);
							if (pkgExists) {
								p = PackageDrop.findPackage(qname);
								if (p != null) {
									p.invalidate(); // What else is invalidated?
								}
							}
							p = Binding.confirmPackage(qname);
						}
						break;
					default:
						LOG.severe("Not handling removal of "
								+ resource.getName());
					}
				}
				if (p == null) {
					if (LOG.isLoggable(Level.FINE))
						LOG.fine(resource.getName()+" not on classpath");
				} else {
					dependencies.markAsChanged(p);
					/*
					if (!p.types.isEmpty()){
						// conservatively reload package dependencies, since 
						// promises need to be added
						//
						// Also creates matchers for scoped promises as a
						// by-product					
						queueForLaterProcessing(p);
					}
					*/
				}
			}
		});
		// Need analyzeCompilationUnit to be called to collect which CUs changed
		if (kind == IResourceDelta.REMOVED) {
			return true;
		}
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
		CUDrop d = SourceCUDrop.queryCU(new EclipseCodeFile(file));
		dependencies.markAsChanged(d);
		return false;
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IResource[] analyzeEnd(IProject p, IAnalysisMonitor monitor) {
		IResource[] results = super.analyzeEnd(p, monitor);
		dependencies.finish();
		IDE.getInstance().clearAdapting();
		return results;
	}
	
	@Override
	public void postBuild(IProject project) {
		dependencies = null;
	}
	
	/**
	 * @param resource
	 * @return The node for the package (declaration)
	 */
	private PackageDrop parsePackageInfo(final IResource resource) {
		final ICompilationUnit icu = (ICompilationUnit) JavaCore.create(resource);
		final IJavaProject project = icu.getJavaProject();
		if (!project.isOnClasspath(icu)) {
			return null;
		}
		parser.setSource(icu);
		parser.setResolveBindings(true);
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		// identify Javadoc comment
		// apply promises to package
		final PackageDeclaration pd = cu.getPackage();
		final String pkgName  = (pd == null) ? "" : pd.getName().getFullyQualifiedName();
		final PackageDrop old = PackageDrop.findPackage(pkgName);
		if (old != null) {
			/*
			System.out.println("PackageDrop: "+old.javaOSFileName);
			for(Drop d : old.getDependents()) {
				System.out.println("\t"+d.getMessage());
			}
			*/
			dependencies.collect(old);
			old.invalidate();
		}
		
		IRNode root = JavaSourceFileAdapter.getInstance().adaptPackage(icu, cu);
		final PackageDrop pkg = Binding.createPackage(pkgName, root);
		dependencies.markAsChanged(pkg);
		
		runVersioned(new AbstractRunner() {
			public void run() {
				try {
					// Copy the source ref/Javadoc from what I just parsed
					// and put it on the package
					String src = icu.getSource();
					if (pd != null) {
						ISrcRef srcRef = SrcRef.getInstance(pd, cu, resource, src);
						pkg.node.setSlotValue(JavaNode.getSrcRefSlotInfo(), srcRef);
					}
					final IRNode top = VisitUtil.getEnclosingCompilationUnit(pkg.node);
					// Look for Javadoc/Java5 annotations
					final ITypeEnvironment te = Eclipse.getDefault().getTypeEnv(getProject());
					AnnotationVisitor v = new AnnotationVisitor(te, pkgName);
					v.doAccept(top);
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		});
		return pkg;
	}

	
	class Dependencies {
		/**
		 * To avoid cycles and duplication
		 */
		private final Set<Drop> checkedDependents = new HashSet<Drop>();
		private final Set<Drop> checkedDeponents = new HashSet<Drop>();
		/**
		 * The set of CUDrops that need to be reprocessed for promises
		 */
		private final Set<CUDrop> reprocess = new HashSet<CUDrop>();
		/**
		 * The set of changed CUDrops
		 */
		private final Set<CUDrop> changed = new HashSet<CUDrop>();
		
		void markAsChanged(CUDrop d) {
			if (d == null) {
				return; // Nothing to do
			}
			changed.add(d);
			collect(d);
		}

		/**
		 * Collects the CUDrops corresponding to d's dependent drops, 
		 * so we can reprocess the promises on those
		 */	
		private void collect(Drop root) {
			if (checkedDependents.contains(root)) {
				return;
			}
			checkedDependents.add(root);
			
			// Find dependent drops
			for(Drop d : root.getDependents()) {
				//System.out.println(root+" <- "+d);
				findCUDropDeponents(d);
				collect(d);
			}				
		}

		/**
		 * Recursively check this drop and its deponents for CUDrops
		 */
		private void findCUDropDeponents(Drop d) {
			if (checkedDeponents.contains(d)) {
				return;
			}
			checkedDeponents.add(d);
			if (d instanceof CUDrop) {
				reprocess.add((CUDrop) d);
			}
			for(Drop deponent : d.getDeponents()) {
				//System.out.println(d+" -> "+deponent);
				findCUDropDeponents(deponent);
			}
		}
		
		void finish() {
			reprocess.removeAll(changed);			
			/*
			for(CUDrop d : changed) {
				System.out.println("Changed:   "+d.javaOSFileName+" "+d.getClass().getSimpleName());
			}		
			for(CUDrop d : reprocess) {
				System.out.println("Reprocess: "+d.javaOSFileName+" "+d.getClass().getSimpleName());
			}
			*/
			IDE.getInstance().setAdapting();
			try {
				for(CUDrop d : reprocess) {
					if (d instanceof PackageDrop) {
						//Nothing else needed 
					} else {		
						// Clear out promise drops
						//System.out.println("Reprocessing "+d.javaOSFileName);
						for(IRNode n : JavaPromise.bottomUp(d.cu)) {
							PromiseDropStorage.clearDrops(n);
						}	
						ConvertToIR.getInstance().registerClass(d.makeCodeInfo());
					}
				}
			} finally {
				IDE.getInstance().clearAdapting();
			}
		}
	}
}
