/*
 * Created on Mar 23, 2005
 *
 */
package edu.cmu.cs.fluid.ide;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.CommonAASTBinder;
import com.surelogic.analysis.IAnalysisReporter;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.test.*;

import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.mvc.examples.SimpleForestApp;
import edu.cmu.cs.fluid.mvc.version.ModificationManager;
import edu.cmu.cs.fluid.mvc.version.ModificationManagerFactory;
import edu.cmu.cs.fluid.mvc.version.VersionSpaceModel;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.QuickProperties;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;

/**
 * @author Edwin
 */
public abstract class IDE {
  public static final boolean debugTypedASTs = false;
  public static final boolean testReloadMemoryLeak = false;

  /**
   * Logger for this class
   */
  private static final Logger LOG = SLLogger.getLogger("IDE");

  public static final QuickProperties.Flag binderFlag =
    new QuickProperties.Flag(LOG, "surelogic.useNativeBinder", "Binder", true);

  private static boolean useNativeBinder() {
    return QuickProperties.checkFlag(binderFlag);
  }

  public static final boolean useNativeBinder = useNativeBinder();

  public static final QuickProperties.Flag testBinderFlag =
    new QuickProperties.Flag(LOG, "surelogic.testBinder", "Test");

  private static boolean testBinder() {
    return QuickProperties.checkFlag(testBinderFlag);
  }

  public static final boolean testBinder = testBinder();
  
  public static final boolean useNativeBinderOnly = useNativeBinder && !testBinder;
  
  public static final QuickProperties.Flag allowMultipleProjectsFlag =
	  new QuickProperties.Flag(LOG, "surelogic.allowMultipleProjects", "Multi", true);

  private static boolean allowMultipleProjects() {
	  return QuickProperties.checkFlag(allowMultipleProjectsFlag);
  }

  public static final boolean allowMultipleProjects = allowMultipleProjects();

  protected static IDE prototype;

  public static IDE getInstance() {
    if (prototype == null) {
      throw new UnsupportedOperationException();
    }
    return prototype;
  }

  /*************************************************************
   * Code to support ???
   **************************************************************/

  private List<String> typesToLoad = new ArrayList<String>();

  public void ensureTypeIsLoaded(String qname) {
    if (qname == null) {
      return;
    }
    typesToLoad.add(qname);
  }

  public Iterable<String> getQueuedTypes() {
    return typesToLoad;
  }

  public void clearTypeQueue() {
    typesToLoad.clear();
  }

  /*************************************************************
   * Code to support polling to see if analysis got cancelled
   **************************************************************/

  private boolean cancelled = false;

  public synchronized void clearCancelled() {
    cancelled = false;
  }

  public synchronized void setCancelled() {
    cancelled = true;
  }

  public synchronized boolean isCancelled() {
    memPolicy.checkIfLowOnMemory();
    return cancelled;
  }

  /*************************************************************
   * Code to support sanity checks
   **************************************************************/

  private boolean adapting = false;

  public void clearAdapting() {
    adapting = false;
  }

  public void setAdapting() {
    adapting = true;
  }

  public boolean isAdapting() {
    return adapting;
  }

  /*************************************************************
   * Code to support a different classpath for each project
   **************************************************************/

  private IClassPath classpath;

  public final void setDefaultClassPath(IClassPath path) {
    if (path == null) {
//      System.out.println("Clearing the classpath");
    } else {
//      System.out.println("Setting it to "+path);
    }
    classpath = path;
    
    ITypeEnvironment tEnv = getTypeEnv(path);
    AASTBinder.setInstance(tEnv == null ? null : new CommonAASTBinder(tEnv));
  }

  /**
   * @return A type environment based on the current class path
   */
  public final ITypeEnvironment getTypeEnv() {
    if (classpath == null) {
      throw new IllegalArgumentException("null classpath");
    }
    return getTypeEnv(classpath);
  }

  public ITypeEnvironment getTypeEnv(IClassPath path) {
    IClassPathContext context = getContext(path);
    return context != null ? context.getTypeEnv() : null;
  }

  private final Map<IClassPath,IClassPathContext> contexts =
    new HashMap<IClassPath,IClassPathContext>();

  public IClassPathContext getContext(IClassPath path) {
    if (path == null) {
      return null;
    }
    IClassPathContext context = contexts.get(path);
    if (context == null) {
      context = newContext(path);
      contexts.put(path, context);
    }
    return context;
  }

  /*************************************************************
   * Code to support IR paging
   **************************************************************/

  protected abstract IClassPathContext newContext(IClassPath path);

  @SuppressWarnings("unchecked")
  public abstract IJavaFileLocator getJavaFileLocator();

  //private IMemoryPolicy memPolicy = DefaultMemoryPolicy.prototype;
  private IMemoryPolicy memPolicy = ThresholdMemoryPolicy.prototype;

  public final IMemoryPolicy getMemoryPolicy() {
    return memPolicy;
  }

  /*************************************************************
   * Code to support setting various properties,
   * especially for selectively loading modules
   **************************************************************/

  protected Properties props;

  public Properties getProperties() {
    return props;
  }

  public static final String AS_NEEDED = "asNeeded";
  public static final String REQUIRED = "required";
  public static final String AS_SOURCE = "asSource";
  public static final String AS_CLASS = "asClass";
  public static final String MODULE_PREFIX = "Module.";
  public static final String MODULE_DECL_PREFIX = "ModuleDecl.";

  public static final String MODULE_AS_NEEDED = MODULE_PREFIX+AS_NEEDED;
  public static final String MODULE_REQUIRED  = MODULE_PREFIX+REQUIRED;
  public static final String MODULE_AS_SOURCE = MODULE_PREFIX+AS_SOURCE;
  public static final String MODULE_AS_CLASS  = MODULE_PREFIX+AS_CLASS;
  public static final String MODULE_DEFAULTS  = MODULE_PREFIX+"defaults";
  
  public static final String LIB_PREFIX = "Library.";
  public static final String LIB_EXCLUDES = LIB_PREFIX+"excludes";

  public abstract void popupWarning(String string);

  /*************************************************************
   * FIX Hack to support Fluid binder
   **************************************************************/

  private List<ICompUnitListener> compUnitListeners = new ArrayList<ICompUnitListener>(1);

  public synchronized final void addCompUnitListener(ICompUnitListener l) {
    if (compUnitListeners.contains(l)) {
      return;
    }
    compUnitListeners.add(l);
  }

  /**
   * Only to be called after canonicalizing an AST
   */
  public final void notifyASTChanged(IRNode cu) {
    for(ICompUnitListener l : new ArrayList<ICompUnitListener>(compUnitListeners)) {
      l.astChanged(cu);
    }
  }

  public final void notifyASTsChanged() {
    for(ICompUnitListener l : new ArrayList<ICompUnitListener>(compUnitListeners)) {
      l.astsChanged();
    }
  }

  /*************************************************************
   * Code to support TestResults
   *************************************************************/

  private final Map<String,ITestOutput> xmlLogs = new HashMap<String,ITestOutput>();
  private ITestOutputFactory factory = SilentTestOutput.factory; // WarningDropOutput.factory;

  public void addTestOutputFactory(ITestOutputFactory f) {
    if (f == null) {
      throw new IllegalArgumentException("null factory passed in");
    }
    if (factory == null) {
      factory = f;
    } else {
      factory = MultiOutput.makeFactory(factory, f);
    }
  }

  public ITestOutput makeLog(String name) {
    ITestOutput log = getLog(name);
    if (log == null && factory != null) {
      /*
      try {
        log = factory.create(name);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
      */
      log = new LazyTestOutput(name);
    }
    return log;
  }

  public ITestOutput getLog(String name) {
    return xmlLogs.get(name);
  }

  public void closeLogs() {
    for(String name : xmlLogs.keySet()) {
      xmlLogs.get(name).close();
    }
    xmlLogs.clear();
  }

  /**
   * Delays creation of the actual ITestOutput
   * until actually needed, thus allowing the
   * factories to be properly updated
   */
  class LazyTestOutput implements ITestOutput {
    final String name;
    ITestOutput delegate = null;

    public LazyTestOutput(String name) {
      this.name = name;
    }

    ITestOutput ensureDelegateExists() {
      if (delegate == null) {
        try {
          delegate = factory.create(name);
          //new Throwable().printStackTrace();
//          System.out.println("Creating "+name+" ("+delegate+")");
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "Couldn't create delegate", e);
          return null;
        }
      }
      return delegate;
    }

    public void close() {
      ensureDelegateExists().close();
    }

    public Iterable<Object> getUnreported() {
      return ensureDelegateExists().getUnreported();
    }

    public void reportError(ITest o, Throwable ex) {
      ensureDelegateExists().reportError(o, ex);
    }

    public void reportFailure(ITest o, String msg) {
      ensureDelegateExists().reportFailure(o, msg);
    }

    public ITest reportStart(ITest o) {
      return ensureDelegateExists().reportStart(o);
    }

    public void reportSuccess(ITest o, String msg) {
      ensureDelegateExists().reportSuccess(o, msg);
    }
  }

  /*************************************************************
   * Code to deal properly with versioning
   *************************************************************/

  private final Era era = new Era(Version.getInitialVersion());
  {
    Version.setDefaultEra(era);
  }

  private final VersionedRegion region = new VersionedRegion();

  protected final Bundle bundle = new Bundle();

  private final VersionSpaceModel vspace = SimpleForestApp.createVersionSpace(
      "Version space for Eclipse", Version.getInitialVersion());

  private final VersionTrackerModel tracker = (VersionTrackerModel) vspace
      .getCursors().elementAt(0);

  private final ModificationManager modManager = new ModificationManagerWrapper(
      ModificationManagerFactory.prototype.create(vspace, tracker));
  {
    try {
      Version v1 = Version.getVersion();
      // get version created by VersionedRegion constructor
      vspace.addVersionNode(Version.getInitialVersion(), v1);
      modManager.getMarker().setVersion(v1);
      // force to build off version created by VR
      if (JJNode.versioningIsOn) {
        PlainIRNode.setCurrentRegion(region);
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Eclipse constructor", ex);
    }
  }

  public VersionedRegion getRegion() {
    return JJNode.versioningIsOn? region : null;
  }

  public Era getEra() {
    return era;
  }

  public VersionTrackerModel getVersionTracker() {
    return tracker;
  }

  private static Object runVersioned(AbstractRunner runner, IDE e) {
    ModificationManager mm = e.modManager;
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Starting to execute AdaptRunner at "
          + mm.getMarker().getVersion());
    }
    //System.out.print("Now at "+mm.getMarker().getVersion());
    mm.executeAtomically(runner);
    /*
     * if (runner.result == null) { LOG.log(Level.SEVERE,"Problem with null
     * result in " + runner); }
     */
    Version.setVersion(mm.getMarker().getVersion());
    return runner.result;
  }

  public static Object runVersioned(AbstractRunner runner) {
    return runVersioned(runner, IDE.getInstance());
  }

  public static Object runAtMarker(AbstractRunner runner) {
    ModificationManager mm = IDE.getInstance().modManager;
    mm.executeAtMarker(runner);
    return runner.result;
  }

  public static boolean isRunningVersioned() {
    return IDE.getInstance().modManager.isExecuting();
  }

  /**
   * Get the latest Version corresponding to Eclipse's workspace
   *
   * If the workspace is modified since the last sync, the Version returned
   * will be the parent of the latest Version (not created yet)
   */
  public static Version getVersion() {
    return IDE.getInstance().modManager.getMarker().getVersion();
  }

  /**
   * Sync the Eclipse workspace to the version below
   *
   * If Eclipse is modified, ...
   *
   * @param v
   *          A version from Eclipse's version space
   */
  public static boolean setVersion(Version v) {
    IDE.getInstance().tracker.setVersion(v);
    return true;
  }

  /**
   * If Eclipse is modified, create a new Version and return it.
   *
   * Otherwise, returns the same as getVersion()
   */
  public static Version getLatestVersion() {
    return IDE.getInstance().tracker.getVersion();
  }

  private final class ModificationManagerWrapper implements ModificationManager {
    private final ModificationManager manager;

    ModificationManagerWrapper(ModificationManager mm) {
      manager = mm;
    }

    public boolean isExecuting() {
      return manager.isExecuting();
    }

    /*
     * Set default era and region to the ones declared here.
     *
     * @see edu.cmu.cs.cspace.mvc.version.ModificationManager#executeAtomically(java.lang.Runnable)
     */
    public void executeAtomically(Runnable r) {
      Version.pushDefaultEra(era);
      if (JJNode.versioningIsOn) {
        PlainIRNode.setCurrentRegion(region);
      }
      try {
        manager.executeAtomically(r);
      } finally {
        Version.popDefaultEra();
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.cs.fluid.mvc.version.ModificationManager#executeAtMarker(java.lang.Runnable)
     */
    public void executeAtMarker(Runnable r) {
      Version.pushDefaultEra(era);
      if (JJNode.versioningIsOn) {
        PlainIRNode.setCurrentRegion(region);
      }
      try {
        manager.executeAtMarker(r);
      } finally {
        Version.popDefaultEra();
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.cs.cspace.mvc.version.ModificationManager#getMarker()
     */
    public VersionTrackerModel getMarker() {
      return manager.getMarker();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.cs.cspace.mvc.version.ModificationManager#getVersionSpace()
     */
    public VersionSpaceModel getVersionSpace() {
      return manager.getVersionSpace();
    }
  }

  public abstract URL getResourceRoot();

  private IAnalysisReporter reporter = IAnalysisReporter.NULL;

  public IAnalysisReporter getReporter() {
    return reporter;
  }

  public IAnalysisReporter setReporter(IAnalysisReporter r) {
    if (r == null || r == reporter) {
      return null;
    }
    IAnalysisReporter old = reporter;
    reporter = r;
    return old;
  }

  protected void clearCaches_internal() {
	  JavaComponentFactory.clearCache();
  }
  
  public final void clearCaches() {
	  for(IClassPathContext c : contexts.values()) {
		  c.getTypeEnv().clearCaches(false);
	  }
	  clearCaches_internal();
  }
  
  public final void clearAll() {
	  for(IClassPathContext c : contexts.values()) {
		  c.getTypeEnv().clearCaches(true);
	  }
	  clearCaches_internal();
  }
}

