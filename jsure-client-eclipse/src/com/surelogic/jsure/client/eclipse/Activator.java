package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ref.Decl;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.serviceability.scan.JSureScanCrashReport;
import com.surelogic.common.ui.DialogTouchNotificationUI;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.jsure.client.eclipse.model.selection.SelectionManager;
import com.surelogic.jsure.client.eclipse.views.source.HistoricalSourceView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IRunnableWithProgress {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.surelogic.jsure.client.eclipse";

  // The shared instance
  private static Activator plugin;

  // Resource bundle.
  // private ResourceBundle resourceBundle;

  /**
   * The constructor
   */
  public Activator() {
    if (plugin != null)
      throw new IllegalStateException(Activator.class.getName() + " instance already exits, it should be a singleton.");
    plugin = this;
  }

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    EclipseUIUtility.startup(this);
  }

  // Used for startup
  @Override
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    monitor.beginTask("Initializing the JSure tool", 7);

    /*
     * "Touch" common-core-eclipse so the logging gets Eclipse-ified.
     */
    SLEclipseStatusUtility.touch(new DialogTouchNotificationUI());
    monitor.worked(1);

    /*
     * Set the scan crash reporter to an Eclipse implementation.
     */
    JSureScanCrashReport.getInstance().setReporter(EclipseScanCrashReporter.getInstance());
    monitor.worked(1);

    /*
     * "Touch" the JSure preference initialization.
     */
    JSurePreferencesUtility.initializeDefaultScope();
    monitor.worked(1);

    EclipseUtility.getProductReleaseDateJob(SLLicenseProduct.JSURE, this).schedule();
    monitor.worked(1);

    SelectionManager.getInstance().load(getSelectionSaveFile());
    monitor.worked(1);

    SwitchToJSurePerspective.getInstance().init();
    monitor.worked(1);
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    try {
      SelectionManager.getInstance().save(getSelectionSaveFile());
      SwitchToJSurePerspective.getInstance().dispose();
    } finally {
      plugin = null;
      super.stop(context);
    }
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  /**
   * Returns the workspace instance.
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  public File getSelectionSaveFile() {
    final IPath pluginState = Activator.getDefault().getStateLocation();
    return new File(pluginState.toOSString() + File.separator + "selections.xml");
  }

  /**
   * Highlight the passed Java code location in both the Eclipse Java editor and
   * the JSure <i>Historical Source</i> view.
   * 
   * @param javaRef
   *          a location in Java code.
   */
  public static void highlightLineInJavaEditor(final IJavaRef javaRef) {
    highlightLineInJavaEditor(javaRef, false);
  }

  public static void highlightLineInJavaEditor(final IJavaRef javaRef, final boolean tryToUseOld) {
    if (javaRef == null)
      return;

    JDTUIUtility.tryToOpenInEditor(javaRef);
    HistoricalSourceView.tryToOpenInEditor(javaRef, tryToUseOld);
  }

  public static void highlightLineInJavaEditor(IDecl decl) {
    JDTUIUtility.tryToOpenInEditor(decl);
    HistoricalSourceView.tryToOpenInEditor(decl, false);
  }

  public static void tryToOpenInEditor(String proj, String pkg, String cu) {
    JDTUIUtility.tryToOpenInEditor(proj, pkg, cu);
    IDecl p = new Decl.ClassBuilder(cu).setParent(new Decl.PackageBuilder(pkg)).build();
    HistoricalSourceView.tryToOpenInEditor(p, false);
  }

  public static String getVersion() {
    return EclipseUtility.getMajorMinorDotVersion(getDefault());
  }
}
