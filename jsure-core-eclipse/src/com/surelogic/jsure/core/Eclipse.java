package com.surelogic.jsure.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IClassPath;
import edu.cmu.cs.fluid.ide.IClassPathContext;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.LoadOperator;
import edu.cmu.cs.fluid.java.promise.LoadPromise;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * The top level interface to the Fluid plug-in adaption capabilities within the
 * Eclipse integrated development environment.
 */
public final class Eclipse extends IDE {

	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("ECLIPSE");

	/**
	 * The shared Eclipse instance.
	 */
	private static Eclipse eclipse;

	/* Used while initializing the fields in the Eclipse instance */
	private static IWorkspaceRoot tempRoot;

	private final IWorkspaceRoot workspaceRoot;

	/*
	 * 
	 * Accessors
	 */
	/**
	 * Returns the shared instance.
	 */
	public static Eclipse getDefault() {
		return eclipse;
	}

	public IWorkspaceRoot getWorkspaceRoot() {
		return workspaceRoot;
	}

	@Override
	public ITypeEnvironment getTypeEnv(IClassPath path) {
		return super.getTypeEnv(path);
		// return tEnv;
	}

	@Override
	protected IClassPathContext newContext(IClassPath path) {
		return (IClassPathContext) path;
	}

	/*
	 * 
	 * Constructor / initializer
	 */
	private Eclipse(IWorkspaceRoot root) {
		prototype = this; // in IDE

		workspaceRoot = root;
		/*
		 * VersionMarkerModel marker = null; try { marker =
		 * VersionMarkerFactory.prototype.create("Eclipse Marker", Version
		 * .getInitialVersion()); } catch (final SlotAlreadyRegisteredException
		 * e) { LOG.log(Level.SEVERE, "Exception while creating marker", e); }
		 */
	}

	public static void initialize() {
		initialize(ResourcesPlugin.getWorkspace().getRoot(), null);
	}

	public static void initialize(Runnable testInit) {
		initialize(ResourcesPlugin.getWorkspace().getRoot(), testInit);
	}

	/**
	 * @param testInit
	 *            Run just after the Eclipse object is created
	 */
	@SuppressWarnings("deprecation")
	public static synchronized void initialize(IWorkspaceRoot root,
			Runnable testInit) {
		// Ensure log4j and other properties items are loaded
		// QuickProperties.getInstance();
		if (tempRoot != null) {
			if (LOG.isLoggable(Level.FINER))
				LOG.finer("Eclipse already initialized");
			// LOG.debug(new Throwable());
			return;
		}
		try {
			tempRoot = root;
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Eclipse being initialized");
			eclipse = new Eclipse(root);
			if (testInit != null) {
				testInit.run();
			}

			// ITypeEnvironment.getBundle();
			JJNode.getBundle();
			JavaNode.getBundle();
			JavaPromise.getBundle();
			new LoadOperator();
			new LoadPromise();
			// PromiseFramework.getInstance().setParser(
			// EclipsePromiseParser.getInstance());
			RegionAnnotation.getInstance();
			//EffectsAnnotation.getInstance();
			//NotNullAnnotation.getInstance();
			//UniquenessAnnotation.getInstance();
			ColorPromises.getInstance();
			//UseTypeWherePossibleAnnotation.getInstance();
			//ScopedPromises.getInstance();
			//MutabilityAnnotation.getInstance();
			ModulePromises.getInstance();

			// EclipsePromiseParser.setup();
			initVersionedStuff(eclipse);

			// Eclipse.declareBindingsOK(Eclipse.getDefault().getRegion());
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Eclipse initialized");
			/*
			 * runVersioned(new AdaptRunner() { public void run() { result =
			 * PromiseConstants.REGION_ELEMENT; LOG.info("PromiseConstants
			 * initialized"); } });
			 */
		} catch (Throwable t) {
			LOG.log(Level.SEVERE, "Exception while initializing Eclipse", t);
		}
	}

	/**
	 * Method initVersionedStuff.
	 * 
	 * @param eclipse
	 */
	private static void initVersionedStuff(Eclipse eclipse) {
		try {
			AbstractRunner runner = new EclipseInitializer(eclipse);
			runVersioned(runner);
		} catch (Throwable t) {
			LOG.log(Level.SEVERE, "Got exception", t);
		}
	}

	/*
	 * 
	 * Utility code
	 */
	/**
	 * Utility method that returns the full OS path and filename for the given
	 * Java file.
	 * 
	 * @param javaFile
	 *            The Java file to determine the OS filename for.
	 * @return The full OS path and filename for <code>javaFile</code>, or the
	 *         string <code>"null"</code> if unknown.
	 */
	public static String toOSFileName(ICompilationUnit javaFile) {
		return toOSFileName(javaFile.getResource());
	}

	public static String toOSFileName(IResource res) {
		String result = "null";
		try {
			result = res.getLocation().makeAbsolute().toOSString();
		} catch (NullPointerException e) {
			// @ ignore;
		}
		return result;
	}

	public static boolean isActiveJavaFile(IResource resource) {
		if (resource instanceof IFile) {
			String data = resource.getFileExtension();
			if (data != null && data.compareToIgnoreCase("java") == 0) {
				IJavaProject proj = JavaCore.create(resource.getProject());
				return (proj.isOnClasspath(resource));
			}
		}
		return false;
	}

	public static ICompilationUnit getActiveJavaFile(IResource resource) {
		if (resource instanceof IFile) {
			String data = resource.getFileExtension();
			if (data != null && data.compareToIgnoreCase("java") == 0) {
				IJavaProject proj = JavaCore.create(resource.getProject());
				if (proj.isOnClasspath(resource)) {
					return JavaCore.createCompilationUnitFrom((IFile) resource);
				}
			}
		}
		return null;
	}

	private static final class EclipseInitializer extends AbstractRunner {

		final Eclipse eclipse;

		EclipseInitializer(Eclipse e) {
			eclipse = e;
		}

		@Override
		public void run() {
			/*
			 * eclipse.tree = SimpleForestApp.createSyntaxForest(JavaNode.tree,
			 * "Model for Java trees");
			 */
			// eclipse.tEnv = new EclipseTypeEnvironment(Eclipse.tempRoot);
			result = eclipse;
		}
	}

	public IProject findFirstJavaProject() {
		// final boolean debug = LOG.isLoggable(Level.FINE);
		IWorkspaceRoot myWorkspaceRoot = workspaceRoot;
		if (myWorkspaceRoot == null) {
			myWorkspaceRoot = Eclipse.getDefault().getWorkspaceRoot();
		}
		IJavaModel javaModel = JavaCore.create(myWorkspaceRoot);
		try {
			IJavaProject[] javaProjects = javaModel.getJavaProjects();
			if (javaProjects.length > 0) {
				return javaProjects[0].getProject();
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return null;
	}

	/**
	 * True if no build yet, or currently building
	 */
	private boolean inProgress = true;
	private Object workLock = new Object();

	public void waitWhileInProgress() {
		synchronized (workLock) {
			while (inProgress) {
				try {
					workLock.wait();
				} catch (InterruptedException e) {
					Thread.interrupted();
				}
			}
		}
	}

	public void setInProgress(boolean workingNow) {
		synchronized (workLock) {
			boolean wasWorking = inProgress;
			inProgress = workingNow;
			if (wasWorking && !workingNow) {
				workLock.notifyAll();
			}
		}
	}

	/**
	 * Maps strings and IResources to canonical nodes (for non-Java files)
	 */
	private Map<Object, IRNode> nodeMap = new HashMap<Object, IRNode>();

	@Override
	protected void clearCaches_internal() {
		super.clearCaches_internal();
		nodeMap.clear();
	}

	public IRNode confirmResourceNode(IResource res, String name) {
		IRNode n = nodeMap.get(res);
		if (n == null) {
			// n = new MarkedIRNode(name);
			n = edu.cmu.cs.fluid.java.promise.TextFile.createNode(name);
			ISrcRef ref = null; // SrcRef.getInstance(0, 0, 0, res, null);
			JavaNode.setSrcRef(n, ref);
			nodeMap.put(name, n);
			nodeMap.put(res, n);
		}
		IRNode n2 = nodeMap.get(name);
		if (n2 == null) {
			nodeMap.put(name, n);
		} else if (!n2.equals(n)) {
			ISrcRef ref = JavaNode.getSrcRef(n2);
			LOG.warning("Resource name " + name
					+ " was mapped to a different file "
					+ ref.getEnclosingFile());
			nodeMap.put(name, n);
		}
		return n;
	}

	public IRNode getResourceNode(String name) {
		return nodeMap.get(name);
	}

	public IRNode getResourceNode(IResource res) {
		return nodeMap.get(res);
	}

	/**
	 * Used to save Eclipse builder properties
	 */
	public Properties setProperties(Properties props) {
		if (props != null) {
			this.props = props;
		} else {
			this.props = new Properties();
		}
		return this.props;
	}

	@Override
	public URL getResourceRoot() {
		File f = new File(EclipseUtility.getDirectoryOf("edu.cmu.cs.fluid"));
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static final boolean testing = XUtil.testing;

	@Override
	public boolean getBooleanPreference(String key) {
		if (testing && IDEPreferences.ALLOW_JAVADOC_ANNOS.equals(key)) {
			// Enable, so we can test it!
			return true;
		}
		return EclipseUtility.getBooleanPreference(key);
	}

	@Override
	public int getIntPreference(String key) {
		return EclipseUtility.getIntPreference(key);
	}

	@Override
	public String getStringPreference(String key) {
		return EclipseUtility.getStringPreference(key);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public IJavaFileLocator getJavaFileLocator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IAnalysisInfo[] getAnalysisInfo() {
		throw new UnsupportedOperationException();
	}
}