package edu.cmu.cs.fluid.analysis.util;

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
import com.surelogic.annotation.parse.SLAnnotationsLexer;
import com.surelogic.annotation.parse.ScopedPromisesLexer;
import com.surelogic.common.logging.SLLogger;

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
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.dependencies.Dependencies;
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
		dependencies = new Dependencies() {
			protected void handlePackage(final PackageDrop pkg) {
				runVersioned(new AbstractRunner() {
					public void run() {
						parsePackagePromises(pkg);
					}
				});	
			}			
			protected void handleType(CUDrop d) {
				ConvertToIR.getInstance().registerClass(d.makeCodeInfo());
			}
		};
	}

	@Override
	public void analyzeBegin(IProject p) {
		super.analyzeBegin(p);
		ScopedPromisesLexer.init();
		SLAnnotationsLexer.init();
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
					case IResourceDelta.REMOVED:
						System.out.println("Removed: "+resource);
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED:						
						p = parsePackageInfo(resource, kind == IResourceDelta.REMOVED);
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
		// Only here to pick up dependencies
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
	private PackageDrop parsePackageInfo(final IResource resource, boolean removed) {
		final ICompilationUnit icu = (ICompilationUnit) JavaCore.create(resource);
		final IJavaProject project = icu.getJavaProject();
		if (!project.isOnClasspath(icu)) {
			return null;
		}
		final PackageDrop old;
		if (removed) {
			old = PackageDrop.findPackage(icu.getHandleIdentifier());
			
			dependencies.markAsChanged(old);
			old.invalidate();
			return null;
		} 
		// Either added or modified
		parser.setSource(icu);
		parser.setResolveBindings(true);
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		// identify Javadoc comment
		// apply promises to package
		final PackageDeclaration pd = cu.getPackage();
		final String pkgName  = (pd == null) ? "" : pd.getName().getFullyQualifiedName();
		old = PackageDrop.findPackage(pkgName);
		if (old != null) {
			/*
			System.out.println("PackageDrop: "+old.javaOSFileName);
			for(Drop d : old.getDependents()) {
				System.out.println("\t"+d.getMessage());
			}
			*/
			dependencies.markAsChanged(old);
			old.invalidate();
		}
		
		IRNode root = JavaSourceFileAdapter.getInstance().adaptPackage(icu, cu);
		final PackageDrop pkg = Binding.createPackage(pkgName, root, icu.getHandleIdentifier());
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
					parsePackagePromises(pkg);
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		});
		return pkg;
	}

	private void parsePackagePromises(final PackageDrop pkg) {
		final IRNode top = VisitUtil.getEnclosingCompilationUnit(pkg.node);
		// Look for Javadoc/Java5 annotations
		final ITypeEnvironment te = Eclipse.getDefault().getTypeEnv(getProject());
		AnnotationVisitor v = new AnnotationVisitor(te, pkg.javaOSFileName);
		v.doAccept(top);
	}
}
