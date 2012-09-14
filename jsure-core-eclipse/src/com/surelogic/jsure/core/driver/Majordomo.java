package com.surelogic.jsure.core.driver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.JSureProperties;
import com.surelogic.common.PeriodicUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.builder.AbstractJavaBuilder;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.PromiseWarningDrop;
import com.surelogic.jsure.core.listeners.ClearProjectListener;
import com.surelogic.jsure.core.listeners.NotificationHub;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * The central controller to notify analysis modules that changes to the state
 * of a Java project have occurred.
 */
public final class Majordomo extends AbstractJavaBuilder implements
		IResourceVisitor, IResourceDeltaVisitor {
	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	/**
	 * Flags if an AST cache is used by Majordomo to speed up analysis.
	 */
	private static final boolean USE_AST_CACHE = true;

	/**
	 * Holds the {@link IProgressMonitor}passed in by calls to {@link #build}.
	 */
	private volatile IProgressMonitor buildMonitor = null;

	/**
	 * Holds the list of analysis modules at the current level during a call to
	 * {@link #build}.
	 */
	private Set<IAnalysisInfo> currentLevel;

	/**
	 * Holds a cache of Eclipse ASTs that have been generated during a single
	 * change to the Eclipse workspace. Designed to speed analyses by not
	 * rebuilding the Eclipse AST many times.
	 */
	private final Map<ICompilationUnit, CompilationUnit> astCache = new WeakHashMap<ICompilationUnit, CompilationUnit>();

	private final Map<IResource, IJavaElement> javaEltCache = new HashMap<IResource, IJavaElement>();

	private final ASTParser parser = ASTParser.newParser(AST.JLS3);

	private final IAnalysisMonitor monitor = new IAnalysisMonitor() {
		@Override
		public void subTask(String name) {
			setProgressSubTaskName(name);
		}
		@Override
		public void worked() {
			showProgress();
		}
	};

	private static class ProjectClosingListner implements
			IResourceChangeListener {

		/**
		 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
		 */
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
				if (event.getResource() instanceof IProject) {
					IProject p = (IProject) event.getResource();
					if (LOG.isLoggable(Level.FINE))
						LOG.fine("project " + p.getName() + " is closing");
					// TODO: This is where all TMS for the project should be
					// invalidated
					// and (if possible) cleared out of memory.
				}
			} else if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
				if (event.getResource() instanceof IProject) {
					IProject p = (IProject) event.getResource();
					if (LOG.isLoggable(Level.FINE))
						LOG.fine("project " + p.getName() + " is closing");
					// TODO: Not sure what to do here.
				}
			}
		}
	}

	/**
	 * Class constructor.
	 */
	public Majordomo() {
		if (LOG.isLoggable(Level.FINE))
			LOG.fine("Majordomo created");
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				new ProjectClosingListner(), IResourceChangeEvent.PRE_CLOSE);

		final Runnable r = new Runnable() {
			@Override
			public void run() {
				if (buildMonitor != null && buildMonitor.isCanceled()) {
					propagateCancel();
				}
			}
		};
		PeriodicUtility.addHandler(r);
	}

	private void propagateCancel() {
		Iterator<IAnalysis> it = getAnalysisModules();
		while (it.hasNext()) {
			IAnalysis a = it.next();
			a.cancel();
		}
	}

	/**
	 * Since we do not persist, we forget our state at startup to trigger a
	 * rebuild for only our builder.
	 * 
	 * @see IncrementalProjectBuilder#startupOnInitialize()
	 */
	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();
		forgetLastBuiltState();
	}

	/**
	 * The core driver {@link Majordomo}uses to invoke analysis modules. This is
	 * the method invoked by Eclipse for a full or incremental build. However,
	 * we can only invoke analysis modules <i>if</i> the Java project compiles
	 * without any errors.
	 * 
	 * @see IncrementalProjectBuilder#build(int, java.util.Map,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {

		instances.put(getProject().getName(), this);
		setProgressMonitor(monitor);

		// System.out.println("Starting build "+kind+" for "+getProject());
		if (args == null) {
			args = new HashMap();
		}
		args.put(DriverConstants.BUILD_KIND, Integer.toString(kind));

		// cache Eclipse provide workspace changes
		IResourceDelta delta = getDelta(getProject());
		if ((kind == IncrementalProjectBuilder.FULL_BUILD)
				|| (kind == IncrementalProjectBuilder.CLEAN_BUILD)
				|| (delta == null)) {
			projectCache.cacheRebuild();
		} else {
			delta.accept(this); // add changes to our per-project resource cache
		}
		doBuild(args);

		// we could return a list of projects for which we require
		// a resource delta the next time we are run -- but we don't
		return null;
	}

	public void buildManually(IProgressMonitor monitor) throws CoreException {
		setProgressMonitor(monitor);
		doBuild(lastArgs);
	}

	static final boolean debug = false;

	private void doBuild(Map<Object, Object> args) throws CoreException {
		try {
			IJavaProject javaProject = JavaCore.create(getProject());
			if (noCompilationErrors(javaProject)
					&& projectCache.hasInterestingFilesToBuild()) {
				// we are OK to do analysis -- no errors
				if (!IDE.useJavac) {
					NotificationHub.notifyAnalysisStarting();
				}
				final long start = System.currentTimeMillis();
				try {
					projectCache.flushCache(args);
				} catch (CoreException e) {
					handleFailure(
							"General problem while doing double-checker analysis",
							e);
				} finally {
					if (debug) {
						long end = System.currentTimeMillis();
						System.err.println("Time to analyze code = "
								+ (end - start) + " ms");
						// System.out.println("Total nodes          = "+AbstractIRNode.getTotalNodesCreated());
					}
					projectCache.reset(); // wipe the cache for this project
				}
			} else {
				if (!IDE.useJavac) {
					NotificationHub.notifyAnalysisPostponed();
				}
			}
			showBuildIsDone();
		} catch (OperationCanceledException e) {
			handleFailure("Analysis cancelled", null);
		} catch (CoreException e) {
			handleFailure("General problem while preparing for"
					+ " double-checker analysis", e);
			throw e;
		} catch (Throwable e) {
			if (XUtil.testing) {
				throw (RuntimeException) e;
			}
			handleFailure("General problem (Throwable) while"
					+ " preparing for double-checker analysis", e);
		} finally {
			if (!IDE.useJavac) {
				NotificationHub.notifyAnalysisCompleted();
			}
		}
	}

	private void handleFailure(String msg, Throwable t) {
		// Clear before creating warning
		ClearProjectListener.clearJSureState();

//		PromiseWarningDrop d = new PromiseWarningDrop();
//		if (t != null) {
//			String msg2 = t.getMessage();
//			if (msg2 == null) {
//				msg2 = t.getClass().getSimpleName();
//			}
//			d.setMessage(msg + ": " + msg2);
//			LOG.log(Level.WARNING, msg, t);
//		} else {
//			d.setMessage(msg);
//			LOG.log(Level.INFO, msg);
//		}
//		d.setCategory(JavaGlobals.PROMISE_SCRUBBER);
	}

	/**
	 * Resource visitor method for a full re-analysis.
	 * 
	 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
	 */
	@Override
	public boolean visit(IResource resource) {
		analyzeResourceCurrentLevel(resource, IResourceDelta.ADDED);
		return true;
	}

	/**
	 * Resource visitor method for caching resources that have changed within
	 * the Eclipse workspace during an incremental build.
	 * 
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	@Override
	public boolean visit(IResourceDelta delta) {
		IResource resource = delta.getResource();
		projectCache.cacheAResource(resource, delta.getKind());
		return true;
	}

	/**
	 * The project resource caches for this builder and its project
	 */
	private final ProjectCache projectCache = new ProjectCache();

	/**
	 * Args used for the last "build" triggered by Eclipse
	 */
	private Map<Object, Object> lastArgs = Collections.emptyMap();

	private static Map<String, Majordomo> instances = new HashMap<String, Majordomo>();

	/**
	 * A class to manage build caching for a specific project. This is needed
	 * because {@link Majordomo}can only invoke analysis modules (on changed
	 * resources ) when a Java project compiles without errors.
	 */
	private class ProjectCache {

		private boolean doRebuild = false;

		private final List<CacheEntry> resourceCache = new LinkedList<CacheEntry>();

		/**
		 * Clears the {@link #resourceCache} list of cached resources unless it
		 * is already empty.
		 */
		private void clearResourceCache() {
			if (!resourceCache.isEmpty())
				resourceCache.clear();
		}

		/**
		 * Reset the project cache (so the object can be reused.)
		 */
		void reset() {
			doRebuild = false;
			clearResourceCache();
		}

		/**
		 * Cache a resource for analysis when the project compiles without
		 * errors.
		 * 
		 * @param resource
		 *            the resource to add to the project resource cache
		 * @param kind
		 *            one of {@link IResourceDelta#ADDED},
		 *            {@link IResourceDelta#REMOVED}, or
		 *            {@link IResourceDelta#CHANGED}
		 */
		void cacheAResource(IResource resource, int kind) {
			if (!doRebuild) { // ignore if we are already rebuild everything
				// remove any duplicates
				for (Iterator<CacheEntry> iter = resourceCache.iterator(); iter
						.hasNext();) {
					CacheEntry element = iter.next();
					if ((element.getResource().equals(resource))
							&& (element.getKind() == kind)) {
						iter.remove();
					}
				}
				// add to the end of the list (so we maintain Eclipse order)
				resourceCache.add(new CacheEntry(resource, kind));
			}
		}

		/**
		 * Cache a full rebuild (i.e., redo all analyses) when the project
		 * compiles without errors.
		 */
		void cacheRebuild() {
			doRebuild = true;
			clearResourceCache();
		}

		/**
		 * Adds Java files that have been recompiled without source changes
		 * (i.e., due to binding changes) to be re-analyzed. This is
		 * accomplished by examining the cached resources to see if any ".class"
		 * files have been modified without a corresponding change to a ".java"
		 * file.
		 */
		void inferJavaFileCompiled() throws JavaModelException {
			List<IResource> toAddResources = new LinkedList<IResource>();
			// don't do anything for a full build
			if (resourceCache == null)
				return;
			for (CacheEntry cacheEntry : resourceCache) {
				if (cacheEntry.getKind() != IResourceDelta.REMOVED) {
					IResource resource = cacheEntry.getResource();
					String extension = resource.getFileExtension();
					if (extension == null || !extension.equals(".class")) {
						continue; // can't be a class file
					}
					IJavaElement jElement = JavaCore.create(resource);
					if (jElement != null) {
						if (jElement instanceof IClassFile) {
							// is a ".class" file
							IClassFile classFile = (IClassFile) jElement;
							IJavaProject prj = classFile.getJavaProject();
							if (prj != null) {
								String fauxJavaFileName = generateFauxJavaFileName(
										classFile, prj);
								boolean inCache = fauxJavaFileNameExistsInCache(fauxJavaFileName);
								if (inCache) {
									// Need to lookup resource in project
									IResource javaFile = lookupResourceForJavaFileName(
											fauxJavaFileName, prj);
									if (javaFile != null) {
										toAddResources.add(javaFile);
										if (LOG.isLoggable(Level.FINE))
											LOG
													.fine("Java file "
															+ fauxJavaFileName
															+ " added to analysis due to class file change");
									}
								}
							} else {
								// Couldn't find the project
								LOG
										.severe("Unable to find the Java project for "
												+ classFile);
							}
						}
					} else {
						// JavaCore returned null attempting to create
						// IJavaElement
						LOG.severe("Unable to create Java element for "
								+ resource);
					}
				}
			}
			// Add new items to our main cache
			for (IResource newFile : toAddResources) {
				resourceCache.add(new CacheEntry(newFile,
						IResourceDelta.CHANGED));
			}
		}

		/**
		 * Checks if the fauxJavaFileName-prefix is already in the cache to be
		 * analyzed (i.e., a change to the ".java" file caused the compiler to
		 * change the ".class" file).
		 * 
		 * @param fauxJavaFileName
		 *            a made up ".java" file name from a changed ".class" file
		 * @return <code>true</code> if the name of a cached file ends with
		 *         fauxJavaFileName, <code>false</code> otherwise
		 */
		private boolean fauxJavaFileNameExistsInCache(String fauxJavaFileName) {
			boolean found = false;
			for (CacheEntry element : resourceCache) {
				if (element.getResource().getFullPath().toString().endsWith(
						fauxJavaFileName)) {
					found = true;
					break;
				}
			}
			return found;
		}

		/**
		 * Infers a source path relative Java filename (i.e., a ".java" file)
		 * from a ".class" file.
		 * 
		 * @param classFile
		 *            the binary ".class" file we want the ".java" name for
		 * @param prj
		 *            the Eclipse Java project the class file is contained
		 *            within
		 * @return a source path relative Java file name for the given
		 *         classfile, <code>null</code> otherwise
		 */
		private String generateFauxJavaFileName(IClassFile classFile,
				IJavaProject prj) throws JavaModelException {
			String result = null; // default if we can't generate a name
			String classFilePath = classFile.getPath().toString();
			// check to ensure that this filename ends with "class"
			if (classFilePath.substring(classFilePath.length() - 5)
					.equalsIgnoreCase("class")) {
				// change ".class" to ".java"
				String javaFilePath = classFilePath.substring(0, classFilePath
						.length() - 5)
						+ "java";
				IPath outputPath = prj.getOutputLocation();
				String outputPathPrefix = outputPath.toString();
				if (javaFilePath.startsWith(outputPathPrefix)) {
					result = javaFilePath.substring(outputPathPrefix.length());
				} else {
					LOG.severe("Eclipse output directory of classfile "
							+ classFilePath
							+ " is not in the default location of "
							+ outputPathPrefix);
				}
			} else {
				if (LOG.isLoggable(Level.FINE))
					LOG.fine("Modified classfile " + classFilePath
							+ "does not end with \".class\"");
			}
			return result;
		}

		/**
		 * Attempts, by examining each and every Eclipse package fragment root,
		 * to lookup the <code>IResource</code> for the given source path
		 * relative ".java" file name. Returns <code>null</code> if the file
		 * cannot be found.
		 * 
		 * @param resourceJavaFilePath
		 *            the Java filename to lookup
		 * @param prj
		 *            the Eclipse Java project the file is contained within
		 * @return the Eclipse resource related to the given filename,
		 *         <code>null</code> otherwise
		 */
		private IResource lookupResourceForJavaFileName(
				String resourceJavaFilePath, IJavaProject prj)
				throws JavaModelException {
			IResource result = null; // default is null
			for (int i = 0; i < prj.getAllPackageFragmentRoots().length; i++) {
				IPackageFragmentRoot curPkgFragRoot = prj
						.getAllPackageFragmentRoots()[i];
				// only check if this package fragment root contains source code
				if (curPkgFragRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
					String srcPath = curPkgFragRoot.getRawClasspathEntry()
							.getPath().toString();
					String javaFileName = srcPath + resourceJavaFilePath;
					IPath javaFilePath = new Path(javaFileName);
					result = prj.getProject().findMember(
							javaFilePath.removeFirstSegments(1));
				}
			}
			return result;
		}

		/**
		 * Engages analysis modules on all changed project resources (which have
		 * been cached) when the project compiles with no errors.
		 * 
		 * @throws CoreException
		 *             if visiting project resource encounters problems
		 */
		void flushCache(Map<Object, Object> args) throws CoreException {
			astCache.clear(); // clear the Eclipse AST cache
			javaEltCache.clear();

			long analysisStartTime = System.currentTimeMillis();
			inferJavaFileCompiled();
			try {
				if (doRebuild) {
					// full re-analysis
					if (LOG.isLoggable(Level.FINE))
						LOG.fine("flushing cache on " + getProject().getName()
								+ " via full analysis");
					CounterVisitor counter = new CounterVisitor();
					getProject().accept(counter);
					int totalWork = counter.getResourceCount()
							* analysisDepth();
					showBuildIsStarting(totalWork);
					preBuild(getProject(), args);
					resetAnalysesForFullBuild(getProject());
					for (int i = 0; i < analysisDepth(); ++i) {
						currentLevel = DoubleChecker.getDefault().m_analysisExtensionSets
								.get(i);

						final long start = System.currentTimeMillis();
						analyzeBeginCurrentLevel(getProject());
						getProject().accept(Majordomo.this);
						analyzeEndCurrentLevel(getProject());
						if (debug) {
							final long end = System.currentTimeMillis();
							System.out
									.println("Time: " + (end - start) + " ms");
							for (IAnalysisInfo ext : currentLevel) {
								System.out.println("\t" + ext.getLabel());
							}
						}
					}
					postBuild(getProject());
				} else {
					// incremental analyses based upon the cached resource delta
					if (LOG.isLoggable(Level.FINE))
						LOG.fine("flushing cache on " + getProject().getName()
								+ " via incremental analysis");
					int totalWork = resourceCache.size() * analysisDepth();
					showBuildIsStarting(totalWork);
					preBuild(getProject(), args);
					for (int i = 0; i < analysisDepth(); ++i) {
						currentLevel = DoubleChecker.getDefault().m_analysisExtensionSets
								.get(i);

						final long start = System.currentTimeMillis();
						analyzeBeginCurrentLevel(getProject());
						for (CacheEntry element : resourceCache) {
							Majordomo.this.analyzeResourceCurrentLevel(element
									.getResource(), element.getKind());
						}
						analyzeEndCurrentLevel(getProject());
						if (debug) {
							final long end = System.currentTimeMillis();
							System.out
									.println("Time: " + (end - start) + " ms");
							for (IAnalysisInfo ext : currentLevel) {
								System.out.println("\t" + ext.getLabel());
							}
						}
					}
					postBuild(getProject());
				}
			} finally {
				long durationMS = System.currentTimeMillis()
						- analysisStartTime;
				if (LOG.isLoggable(Level.FINE))
					LOG.fine("double-checker took " + durationMS + " ms");
				astCache.clear(); // clear the Eclipse AST cache
				javaEltCache.clear();

				if (LOG.isLoggable(Level.FINE))
					LOG.fine("Ended using " + DoubleChecker.memoryUsed()
							+ " bytes");
			}
		}

		boolean hasInterestingFilesToBuild() {
			if (doRebuild) {
				return true;
			}
			for (CacheEntry e : resourceCache) {
				if (isInteresting(e)) {
					return true;
				}
			}
			return false;
		}

		private boolean isInteresting(CacheEntry e) {
			if (e.resource.getParent() instanceof IProject) {
				return isFluidProperties(e.resource)
						|| isDotProject(e.resource)
						|| isDotClasspath(e.resource) || false;
			} else if (isOnClassPath(e.resource)) {
				return isJavaSource(e.resource) || isPromisesXML(e.resource)
						|| false;
			}
			return false;
		}

		public boolean isPromisesXML(IResource resource) {
			return (resource.getType() == IResource.FILE && resource.getName()
					.endsWith(TestXMLParserConstants.SUFFIX));
		}

		public boolean isJavaSource(IResource resource) {
			return (resource.getType() == IResource.FILE && resource.getName()
					.endsWith(".java"));
		}

		public boolean isDotProject(IResource resource) {
			return (resource.getType() == IResource.FILE && resource
					.getFullPath().toString().equals(".project"));
		}

		public boolean isDotClasspath(IResource resource) {
			return (resource.getType() == IResource.FILE && resource
					.getFullPath().toString().equals(".classpath"));
		}

		public boolean isFluidProperties(IResource resource) {
			final String name = resource.getFullPath().toString();
			return (resource.getType() == IResource.FILE && name
					.equals(JSureProperties.JSURE_PROPERTIES));
		}

		private boolean isOnClassPath(IResource resource) {
			final IJavaElement jElement = JavaCore.create(resource);
			if (jElement != null) {
				final IJavaProject prj = jElement.getJavaProject();
				if (prj != null) {
					final String resPath = resource.getFullPath().toString();
					try {
						for (IPackageFragmentRoot root : prj
								.getAllPackageFragmentRoots()) {
							if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
								String srcPath = root.getRawClasspathEntry()
										.getPath().toString();
								if (resPath.startsWith(srcPath)) {
									return true;
								}
							}
						}
					} catch (JavaModelException e) {
						LOG.log(Level.SEVERE, "Error trying to find "
								+ resource + " on classpath", e);
					}
				}
			}
			return false;
		}
	}

	/**
	 * A simple class to save a single {@link IResource}and what type of change
	 * (e.g., {@link IResourceDelta#ADDED},{@link IResourceDelta#REMOVED}, or
	 * {@link IResourceDelta#CHANGED}) was done to it within the project
	 * workspace.
	 */
	private static class CacheEntry {

		private IResource resource;

		private int kind;

		/**
		 * Constructs a fully defined new project resource builder cache entry.
		 * 
		 * @param resource
		 *            the resource
		 * @param kind
		 *            one of {@link IResourceDelta#ADDED},
		 *            {@link IResourceDelta#REMOVED}, or
		 *            {@link IResourceDelta#CHANGED}
		 */
		CacheEntry(IResource resource, int kind) {
			this.resource = resource;
			this.kind = kind;
		}

		/**
		 * @return one of {@link IResourceDelta#ADDED},
		 *         {@link IResourceDelta#REMOVED}, or
		 *         {@link IResourceDelta#CHANGED}
		 */
		int getKind() {
			return kind;
		}

		/**
		 * @return cached resource
		 */
		IResource getResource() {
			return resource;
		}
	}

	/**
	 * Returns the number of distinct levels, or passes, needed to properly
	 * invoke all registered analysis modules and respect their prerequisites.
	 * 
	 * @return number of levels of analysis needed
	 */
	public int analysisDepth() {
		// how many levels do we have
		return DoubleChecker.getDefault().m_analysisExtensionSets.size();
	}

	/**
	 * Informs all registered analysis modules that a build is starting for a
	 * specific Eclipse project.
	 * 
	 * @param project
	 *            the Eclipse project about to be built
	 */
	private void preBuild(IProject project, Map<Object, Object> args) {
		DoubleChecker plugin = DoubleChecker.getDefault();
		for (IAnalysisInfo ext : plugin.analysisExtensions) {
			IAnalysis analysisModule = DoubleChecker.getDefault()
					.getAnalysisModule(ext);
			analysisModule.setLabel(ext.getLabel());
			analysisModule.preBuild(project);
			analysisModule.setArguments(args);
		}
	}

	private Iterator<IAnalysis> getAnalysisModules() {
		final DoubleChecker plugin = DoubleChecker.getDefault();
		final IAnalysisInfo[] extensions = plugin.analysisExtensions;
		return new Iterator<IAnalysis>() {
			int i = 0;

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			@Override
			public boolean hasNext() {
				return i < extensions.length;
			}
			@Override
			public IAnalysis next() {
				IAnalysis a = plugin.getAnalysisModule(extensions[i++]);
				return a;
			}
		};
	}

	/**
	 * Informs all registered analysis modules that a rebuild is going to be
	 * done for a specific Eclipse project.
	 * 
	 * @param project
	 *            the Eclipse project about to be rebuilt
	 */
	private void resetAnalysesForFullBuild(IProject project) {
		DoubleChecker plugin = DoubleChecker.getDefault();
		IAnalysisInfo[] extensions = plugin.analysisExtensions;
		for (int i = 0; i < extensions.length; ++i) {
			plugin.getAnalysisModule(extensions[i]).resetForAFullBuild(project);
		}
	}

	/**
	 * Invokes {@link IAnalysis#analyzeBegin}on all analysis modules at the
	 * current level.
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 */
	public void analyzeBeginCurrentLevel(IProject project) {
		for (IAnalysisInfo ext : currentLevel) {
			IAnalysis module = DoubleChecker.getDefault()
					.getAnalysisModule(ext);
			long start = System.nanoTime();
			module.analyzeBegin(project);
			long end = System.nanoTime();
			long time = (end - start) / 1000000;
			if (time > 1000) {
				if (LOG.isLoggable(Level.FINE))
					LOG.fine(ext.getLabel() + " analyzeBegin: " + time + " ms");
			}
		}
	}

	/**
	 * Invokes {@link IAnalysis#analyzeResource}on all analysis modules at the
	 * current level.
	 * 
	 * @param resource
	 *            the resource within the project to analyze
	 * @param kind
	 *            one of {@link IResourceDelta#ADDED},
	 *            {@link IResourceDelta#REMOVED}, or
	 *            {@link IResourceDelta#CHANGED}
	 */
	private void analyzeResourceCurrentLevel(IResource resource, int kind) {
		for (IAnalysisInfo ext : currentLevel) {
			IAnalysis analysisModule = DoubleChecker.getDefault()
					.getAnalysisModule(ext);
			if (LOG.isLoggable(Level.FINE)) {
				long start = System.nanoTime();
				analyzeResource(analysisModule, resource, kind);
				long end = System.nanoTime();
				long time = (end - start) / 1000000;
				if (time > 500) {
					LOG.fine(ext.getLabel() + " on " + resource.getName()
							+ ": " + time + " ms");
				}
			} else {
				analyzeResource(analysisModule, resource, kind);
			}
		}
		showProgress();
		checkForUserCancel();
	}

	/**
	 * Invokes {@link IAnalysis#analyzeResource}on <code>analysisModule</code>
	 * and checks if {@link IAnalysis#analyzeCompilationUnit}needs to be
	 * invoked.
	 * 
	 * @param analysisModule
	 *            the analysis module target
	 * @param resource
	 *            the resource to analyze
	 * @param kind
	 *            one of {@link IResourceDelta#ADDED},
	 *            {@link IResourceDelta#REMOVED}, or
	 *            {@link IResourceDelta#CHANGED}
	 */
	private void analyzeResource(IAnalysis analysisModule, IResource resource,
			int kind) {

		if (LOG.isLoggable(Level.FINE)) {
			String msg = "Checking [" + analysisModule.getLabel() + "] "
					+ resource.getName();
			setProgressSubTaskName(msg);
			LOG.fine(msg);
		}
		boolean sendCompilationUnit = !analysisModule.analyzeResource(resource,
				kind);
		if (sendCompilationUnit) {
			sendCompilationUnit(analysisModule, resource);
		}
	}

	/**
	 * Invokes {@link IAnalysis#analyzeCompilationUnit}on
	 * <code>analysisModule</code> if <code>resource</code> is a Java
	 * compilation unit that is on the project classpath.
	 * 
	 * @param analysisModule
	 *            the analysis module target
	 * @param resource
	 *            the resource to analyze
	 */
	private void sendCompilationUnit(IAnalysis analysisModule,
			IResource resource) {
		IJavaElement javaResource = javaEltCache.get(resource);
		if (javaResource == null) {
			javaResource = JavaCore.create(resource);
			javaEltCache.put(resource, javaResource);
		}

		if ((javaResource != null)
				&& (javaResource.getElementType() == IJavaElement.COMPILATION_UNIT)) {
			ICompilationUnit compUnit = (ICompilationUnit) javaResource;
			if (compUnit.getJavaProject().isOnClasspath(compUnit)) {
				CompilationUnit ast = null;
				if (analysisModule.needsAST()) {
					// check if the Eclipse AST has been cached (generated
					// before)
					ast = astCache.get(compUnit);
					if (ast == null) {
						// Not in the cache, generate the Eclipse AST and add it
						// to our
						// cache
						parser.setSource(compUnit);
						parser.setResolveBindings(true);
						ast = (CompilationUnit) parser.createAST(null);
						if (USE_AST_CACHE) {
							if (ast != null)
								astCache.put(compUnit, ast);
						}
					}
				}
				String msg = "Checking [" + analysisModule.getLabel() + "] "
						+ resource.getName();
				setProgressSubTaskName(msg);
				analysisModule.analyzeCompilationUnit(compUnit, ast, monitor);
			}
		}
	}

	/**
	 * Invokes {@link #analyzeEnd}on all analysis modules at the current level.
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 */
	private void analyzeEndCurrentLevel(IProject project) throws CoreException {
		for (IAnalysisInfo ext : currentLevel) {
			IAnalysis analysisModule = DoubleChecker.getDefault()
					.getAnalysisModule(ext);
			long start = System.nanoTime();
			analyzeEnd(project, analysisModule);
			long end = System.nanoTime();
			long time = (end - start) / 1000000;
			if (time > 1000) {
				if (LOG.isLoggable(Level.FINE))
					LOG.fine(ext.getLabel() + " analyzeEnd: " + time + " ms");
			}
		}
	}

	/**
	 * Invokes {@link IAnalysis#analyzeEnd}on <code>analysisModule</code> and
	 * performs any reanalysis requested by the analysis module. If reanalysis
	 * is done then recursive calls are made to this method.
	 * 
	 * @param project
	 *            the Eclipse project referenced
	 * @param analysisModule
	 *            the analysis module targeted
	 */
	private void analyzeEnd(IProject project, IAnalysis analysisModule)
			throws CoreException {
		final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);
		IResource[] reanalyze = analysisModule.analyzeEnd(project, monitor);
		if (reanalyze == null) {
			if (fineIsLoggable) {
				LOG
						.fine("Re-analysis of " + project + " for "
								+ analysisModule);
			}
			project.accept(new ReanalysisVisitor(analysisModule));
			analyzeEnd(project, analysisModule); // recursive call
		} else if (reanalyze.length > 0) {
			if (fineIsLoggable) {
				LOG.fine("Targeted re-analysis of " + project + " for "
						+ analysisModule);
			}
			for (int j = 0; j < reanalyze.length; j++) {
				if (reanalyze[j] == null) {
					LOG
							.warning("Ignoring null in resources to be reanalyzed by "
									+ analysisModule + ": " + j);
					continue;
				}
				analyzeResource(analysisModule, reanalyze[j],
						IResourceDelta.ADDED);
			}
			analyzeEnd(project, analysisModule); // recursive call
		}
	}

	/**
	 * Informs all registered analysis modules that a build is finished for a
	 * specific Eclipse project.
	 * 
	 * @param project
	 *            the Eclipse project just built
	 */
	private void postBuild(IProject project) {
		DoubleChecker plugin = DoubleChecker.getDefault();
		IAnalysisInfo[] extensions = plugin.analysisExtensions;
		for (int i = 0; i < extensions.length; ++i) {
			plugin.getAnalysisModule(extensions[i]).postBuild(project);
		}
	}

	/**
	 * Used by {@link #flushCache}to count the number of resources in the entire
	 * project. Haven't found a better way (i.e., more efficient) way to
	 * accomplish this.
	 */
	private static class CounterVisitor implements IResourceVisitor {

		int resourceCount = 0;

		/**
		 * Provides the result of counting the resources within a project.
		 * 
		 * @return the number of resources found in the visited project
		 */
		int getResourceCount() {
			return resourceCount;
		}

		/**
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		@Override
		public boolean visit(IResource resource) {
			resourceCount++;
			return true;
		}
	}

	/**
	 * Used by {@link #analyzeEnd}to support reanalysis of an entire project.
	 * This class is needed because the main {@link IResourceVisitor}implemented
	 * by {@link Majordomo}targets <I>all </I> analysis modules rather than a
	 * specified single analysis module.
	 */
	private class ReanalysisVisitor implements IResourceVisitor {

		/**
		 * The analysis module to invoke method calls on.
		 */
		private IAnalysis analysisModule;

		ReanalysisVisitor(IAnalysis targetAnalysisModule) {
			analysisModule = targetAnalysisModule;
		}

		/**
		 * Invokes the {@link IAnalysis#analyzeResource}method on the analysis
		 * module stored in the field {@link #analysisModule}for
		 * <code>resource</code>.
		 * 
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		@Override
		public boolean visit(IResource resource) {
			analyzeResource(analysisModule, resource, IResourceDelta.ADDED);
			return true;
		}
	}

	/**
	 * Sets the value of the {@link #buildMonitor}field to a progress monitor.
	 * 
	 * @param monitor
	 *            the {@link IProgressMonitor}reference
	 */
	private void setProgressMonitor(final IProgressMonitor monitor) {
		buildMonitor = monitor;
	}

	/**
	 * Sets up the {@link #buildMonitor}for our portion of the build.
	 * 
	 * @param totalWork
	 *            number of <I>work units </I> (i.e., files) we are going to
	 *            examine.
	 */
	private void showBuildIsStarting(int totalWork) {
		if (buildMonitor != null) {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Progress monitor work set to " + totalWork);
			buildMonitor.beginTask("JavaAssure", totalWork);
		}
	}

	/**
	 * Informs {@link #buildMonitor}that one <I>work unit </I> (i.e., a file)
	 * has been completed.
	 */
	private void showProgress() {
		if (buildMonitor != null) {
			buildMonitor.worked(1);
		}
	}

	/**
	 * Sets the {@link #buildMonitor}sub task name (i.e., a file) in the
	 * progress monitor dialog.
	 */
	private void setProgressSubTaskName(String msg) {
		if (buildMonitor != null) {
			buildMonitor.subTask(msg);
		}
	}

	/**
	 * Checks of the {@link IProgressMonitor}referenced by the
	 * {@link #buildMonitor}field indicates that the user has cancelled the
	 * build. If so we forget our build state and throw a
	 * {@link OperationCanceledException}.
	 * 
	 */
	private void checkForUserCancel() {
		if (buildMonitor != null) {
			if (buildMonitor.isCanceled()) {
				// the below line was removed for Eclipse 3.0 background builds
				// for some reason cancel caused a new build to start
				// reference: Fluid Bugzilla #184
				// forgetLastBuiltState();
				if (LOG.isLoggable(Level.FINE))
					LOG.fine("double-checker cancelled");
				throw new OperationCanceledException();
			}
		}
	}

	/**
	 * Shows that a build is complete by updating our {@link #buildMonitor}.
	 */
	private void showBuildIsDone() {
		buildMonitor.done();
		buildMonitor = null;
	}

	/**
	 * Check if the compilation state of an {@link IJavaProject} has errors.
	 * 
	 * @param javaProject
	 *            the {@link IJavaProject}to check for errors
	 * @return <code>true</code> if the project has no compilation errors,
	 *         <code>false</code> if errors exist or the project has never been
	 *         built
	 * @throws CoreException
	 *             if we have trouble getting the project's {@link IMarker}list
	 */
	static boolean noCompilationErrors(IJavaProject javaProject)
			throws CoreException {
		boolean result = false; // assume it has errors or has never been built
		if (javaProject.hasBuildState()) {
			result = true; // OK, we have a build state so assume no errors
			IMarker[] problems = javaProject.getProject().findMarkers(
					// IMarker.PROBLEM,
					IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
					IResource.DEPTH_INFINITE);
			// check if any of these have a severity attribute that indicates an
			// error
			for (IMarker marker : problems) {
				if (IMarker.SEVERITY_ERROR == marker.getAttribute(
						IMarker.SEVERITY, IMarker.SEVERITY_INFO)) {
					LOG.fine("***** MARKER Message: "
							+ marker.getAttribute(IMarker.MESSAGE));
					LOG.fine("***** MARKER Line #: "
							+ marker.getAttribute(IMarker.LINE_NUMBER));
					LOG.fine("***** MARKER File: "
							+ marker.getAttribute(IMarker.LOCATION));
					LOG.fine("***** MARKER Message: "
							+ marker.getAttribute(IMarker.MESSAGE));
					return false; // we found an error (bail out)
				}
			}
		} else {
			LOG.info("NO BUILD STATE");
		}
		return result;
	}

	static void analyze(IProject project, IProgressMonitor monitor)
			throws CoreException {
		Majordomo m = instances.get(project.getName());
		if (m != null) {
			m.buildManually(monitor);
		} else {
			LOG.severe("No Majordomo for " + project.getName());
		}

	}

	public static void analyzeNow(final boolean isAuto) {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceDescription description = workspace.getDescription();
		for (String proj : description.getBuildOrder()) {
			final IProject project = EclipseUtility.getProject(proj);
			// TODO Are these run in order?
			final String msg = isAuto ? "Automatic JSure analysis of "
					+ project.getName() : "On-demand JSure analysis of "
					+ project.getName();

			new FirstTimeJob(msg, project) {
				@Override
				protected void doJob(IProgressMonitor monitor)
						throws CoreException {
					Majordomo.analyze(project, monitor);
				}
			}.schedule();
		}
	}
}