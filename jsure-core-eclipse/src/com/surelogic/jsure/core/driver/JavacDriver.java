package com.surelogic.jsure.core.driver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.JSureProperties;
import com.surelogic.annotation.rules.ModuleRules;
import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.FileUtility;
import com.surelogic.common.FileUtility.TempFileFilter;
import com.surelogic.common.Pair;
import com.surelogic.common.PeriodicUtility;
import com.surelogic.common.TextArchiver;
import com.surelogic.common.XUtil;
import com.surelogic.common.ZipInfo;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.java.*;
import com.surelogic.common.core.jobs.EclipseLocalConfig;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.remote.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.serviceability.scan.JSureScanCrashReport;
import com.surelogic.dropsea.ir.utility.ClearStateUtility;
import com.surelogic.dropsea.irfree.ISeaDiff;
import com.surelogic.dropsea.irfree.SeaSnapshotDiff;
import com.surelogic.javac.Javac;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;
import com.surelogic.javac.jobs.LocalJSureJob;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.javac.persistence.ScanProperty;
import com.surelogic.jsure.core.listeners.NotificationHub;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureDataDirHub.CurrentScanChangeListener;
import com.surelogic.jsure.core.scripting.ExportResults;
import com.surelogic.jsure.core.scripting.ICommandContext;
import com.surelogic.jsure.core.scripting.NullCommand;
import com.surelogic.jsure.core.scripting.ScriptCommands;
import com.surelogic.jsure.core.scripting.ScriptReader;
import com.surelogic.xml.TestXMLParserConstants;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JavacDriver extends AbstractJavaScanner<Projects,JavacProject> implements IResourceChangeListener, CurrentScanChangeListener {
  private static final String SCRIPT_TEMP = "scriptTemp";
  private static final String CRASH_FILES = "crash.log.txt";

  // private static final Logger LOG =
  // SLLogger.getLogger("analysis.JavacDriver");

  /**
   * Clear all the JSure state before each build
   */
  private static final boolean clearBeforeAnalysis = false;

  // private final List<IProject> building = new ArrayList<IProject>();

  /**
   * Only used for scripting
   */
  private final File tempDir;
  private final File scriptResourcesDir;
  private final PrintStream script;
  private final ZipInfo info;
  private boolean ignoreNextCleanup = true;
  /*
   * Only used for updating a script
   */
  private final SLJob updateScriptJob;
  private final Map<String, Long> deleted;
  private final File deletedDir;

  private JavacDriver() {
	super(Projects.javaFactory);
    PeriodicUtility.addHandler(new Runnable() {
      @Override
      public void run() {
        final SLProgressMonitor mon = lastMonitor;
        if (mon != null && mon.isCanceled()) {
          if (lastMonitor == mon) {
            lastMonitor = null;
          }
          IDE.getInstance().setCancelled();
        }
      }
    });
    final String update = XUtil.updateScript();
    final String temp = XUtil.recordScript();
    final File scriptBeingUpdated;
    if (update != null) {
      // Backup the zip to be updated
      final String prefix = temp;
      final File workspace = EclipseUtility.getWorkspacePath();
      File archive = new File(update);
      if (!archive.exists()) {
        // No absolute path, so look for it in the workspace
        archive = new File(workspace, update);
      }
      if (!archive.exists()) {
        throw new IllegalStateException("Doesn't exist: " + archive);
      }
      final File backup = new File(workspace, prefix + ".bak.zip");
      if (backup.exists()) {
        backup.delete();
      }
      FileUtility.copy(archive, backup);
      try {
        // Delete any project with the given name
        IProject p = EclipseUtility.getProject(prefix);
        if (p != null) {
          p.delete(true, true, null);
        }
        final File proj = new File(workspace, prefix);
        if (proj.exists()) {
          if (!FileUtility.recursiveDelete(proj)) {
            throw new IllegalStateException("Unable to delete project: " + proj);
          }
        }
        /*
         * The zip already contains the project as part of the path, so don't do
         * the following:
         * 
         * proj.mkdirs();
         * 
         * // Unzip into the project FileUtility.unzipFile(archive, proj);
         */
        // Unzip into the workspace
        FileUtility.unzipFile(archive, workspace);

        // Make a copy of the script to use while updating
        FileUtility.deleteTempFiles(scriptFilter);
        scriptBeingUpdated = scriptFilter.createTempFile();
        FileUtility.copy(new File(proj, ScriptCommands.NAME), scriptBeingUpdated);

        // Make a directory to keep the "deleted" files
        FileUtility.deleteTempFiles(deletedDirFilter);
        deletedDir = deletedDirFilter.createTempFolder();

        // Import the project into the workspace
        importScriptedProject(proj);

        // Refresh the workspace
        ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);

        JavacEclipse.initialize();
        ((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs();
      } catch (Exception e) {
        throw new IllegalStateException("Could not create/import project", e);
      }
      deleted = new HashMap<String, Long>();
      // After this, we should be able to re-script the project like
      // before
    } else {
      deleted = null;
      deletedDir = null;
      scriptBeingUpdated = null;
    }
    // There is a script to create
    if (temp != null) {
      final int slash = temp.indexOf('/');
      final String proj, path;
      if (slash < 0) {
        proj = temp;
        path = temp + File.separatorChar + "script";
      } else {
        proj = temp.substring(0, slash);
        path = temp;
      }
      final File workspace = EclipseUtility.getWorkspacePath();
      scriptResourcesDir = new File(workspace, path);
      scriptResourcesDir.mkdirs();
      if (update == null) {
        // Clean out the directory
        for (File f : scriptResourcesDir.listFiles()) {
          FileUtility.recursiveDelete(f);
        }
      } else {
        // Doing an update, so just delete expected sea.xml
        for (File f : scriptResourcesDir.listFiles(updateFilter)) {
          deleted.put(f.getName(), f.length());
          f.renameTo(new File(deletedDir, f.getName()));
          FileUtility.recursiveDelete(f);
        }
        File testProps = new File(workspace, proj + File.separatorChar + ScriptCommands.TEST_PROPERTIES);
        if (testProps.exists()) {
          // This will be re-created later from the actual settings
          // used
          testProps.delete();
        }
      }
      FileUtility.deleteTempFiles(filter);
      File tmp = null; 
      try {
    	  tmp = filter.createTempFolder();
      } catch (IOException e) {
          e.printStackTrace();
      }
      tempDir = tmp;
      
      PrintStream out = null;
      ZipInfo zipInfo = null;
      final File scriptF = new File(workspace, proj + File.separatorChar + ScriptCommands.NAME);
      try {
        if (update == null) {
          if (scriptF.exists()) {
            System.out.println("Deleting old script " + scriptF);
            scriptF.delete();
          }
        }
        final File zip = new File(workspace, proj + ".zip");
        if (zip.exists()) {
          System.out.println("Deleting existing " + zip);
          zip.delete();
        }
        // Zip up most of the project before it gets changed
        zipInfo = FileUtility.zipDirAndMore(workspace, new File(workspace, proj), zip);
        if (update == null) {
          out = new PrintStream(scriptF);
        }

        IJavaProject jp = JDTUtility.getJavaProject(proj);
        boolean cached = false;
        if (jp != null) {
          loadFileCache(jp);
          cached = true;
        } else {
          for (IJavaProject p : JDTUtility.getJavaProjects()) {
            try {
              String projPath = p.getCorrespondingResource().getLocation().toOSString();
              if (projPath.contains(proj)) {
                loadFileCache(p);
                cached = true;
              }
            } catch (JavaModelException e) {
              e.printStackTrace();
            }
          }
        }
        if (!cached) {
        	System.err.println("Unable to cache project "+proj);
        } else {
        	System.out.println("Starting scripting for project "+proj);
        }
        JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
      } catch (IOException e) {
        e.printStackTrace();
      }
      script = (tempDir == null) ? null : out;
      info = zipInfo;

      if (scriptBeingUpdated != null) {
        final File scriptDir = new File(workspace, temp);
        updateScriptJob = new AbstractSLJob("Updating script") {
          @Override
          public SLStatus run(SLProgressMonitor monitor) {
            try {
              // TODO needs to run after the FTA auto-build
              final UpdateScriptReader r = new UpdateScriptReader(scriptDir);
              return r.execute(scriptBeingUpdated, scriptDir) ? SLStatus.OK_STATUS : SLStatus.CANCEL_STATUS;
            } catch (Throwable e) {
              return SLStatus.createErrorStatus(e);
            }
          }
        };
      } else {
        updateScriptJob = null;
      }
    } else {
      script = null;
      scriptResourcesDir = null;
      tempDir = null;
      info = null;
      updateScriptJob = null;
    }
  }

  private void loadFileCache(IJavaProject proj) {
    if (proj == null) {
      return;
    }
    final List<ICompilationUnit> cus = new ArrayList<ICompilationUnit>();
    try {
      for (IPackageFragment frag : proj.getPackageFragments()) {
        for (ICompilationUnit cu : frag.getCompilationUnits()) {
          cus.add(cu);
        }
      }
      cacheCompUnits(cus);
    } catch (JavaModelException e) {
      e.printStackTrace();
    }

  }

  private static void importScriptedProject(final File proj) throws CoreException {
    File dotProject = new File(proj, EclipseUtility.DOT_PROJECT);
    if (dotProject.exists()) {
      EclipseUtility.importProject(proj);
    } else {
      for (File dir : proj.listFiles()) {
        if (dir.isDirectory()) {
          importScriptedProject(dir);
        }
      }
    }
  }

  private static List<IJavaProject> findProjects(final File proj) {
    File dotProject = new File(proj, EclipseUtility.DOT_PROJECT);
    if (dotProject.exists()) {
      return Collections.singletonList(JDTUtility.getJavaProject(proj.getName()));
    } else {
      List<IJavaProject> rv = new ArrayList<IJavaProject>();
      for (File dir : proj.listFiles()) {
        if (dir.isDirectory()) {
          rv.addAll(findProjects(dir));
        }
      }
      return rv;
    }
  }

  public void updateScript() {
    if (updateScriptJob != null) {
      try {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.AUTO_BUILD, null);
      } catch (CoreException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      // This is in a job to let it run after plugins are initialized
      EclipseUtility.toEclipseJob(new AbstractSLJob("Preparing to run update script job") {
        @Override
        public SLStatus run(SLProgressMonitor monitor) {
          // This cannot lock the workspace, since it prevents
          // builds from happening
          System.out.println("Running update script job");
          EclipseUtility.toEclipseJob(updateScriptJob).schedule();
          return SLStatus.OK_STATUS;
        }
      }).schedule();
    }
  }

  private static class UpdateScriptReader extends ScriptReader {
    public UpdateScriptReader(final File proj) {
      super(findProjects(proj), false);
      // These should be the ops that we auto-inserted
      commands.put(ScriptCommands.EXPECT_BUILD, NullCommand.prototype);
      commands.put(ScriptCommands.EXPECT_ANALYSIS, NullCommand.prototype);
      commands.put(ScriptCommands.COMPARE_RESULTS, new ExportResults() {
        @Override
        public boolean execute(ICommandContext context, String... contents) throws Exception {
          // Reformat the contents for what it expects
          return super.execute(context, contents[0], projects.get(0).getElementName(),
          // Compensate for extra directory when there's multiple
          // projects
              projects.size() > 1 ? ".." + contents[2] : contents[2]);
        }
      });
    }

    @Override
    protected void finish() {
      // PlatformUI.getWorkbench().close();
      String msg = "JSure is done re-running your script.  No actions are being recorded."
          + "Please shutdown Eclipse to finish the archive.";
      // BalloonUtility.showMessage("Safe to shutdown", msg);
      System.err.println(msg);
    }
  }

  private void printToScript(String line) {
    script.println(line);
    script.flush();
  }

  /**
   * Copy the resource to the script resources directory
   * 
   * @return The relative path to the copy
   */
  private String copyAsResource(File targetDir, IResource r) {
    return copyAsResource(targetDir, r, false);
  }

  private String copyAsResource(File targetDir, IResource r, boolean force) {
    File copy = computeCopyFile(targetDir, r);
    if (copy.exists()) {
      if (force) {
        copy.delete();
      } else {
        // FIX uniquify the name
        System.out.println("Already created a copy: " + r.getFullPath());
        return null;
      }
    } else {
      copy.getParentFile().mkdirs();
    }
    FileUtility.copy(r.getLocation().toFile(), copy);
    return r.getFullPath().toString();
  }

  private File computeCopyFile(File targetDir, IResource r) {
    return new File(targetDir, r.getFullPath().toString());
  }

  /**
   * Copy them to the temp directory
   * 
   * @param cus
   */
  private void cacheCompUnits(List<ICompilationUnit> cus) {
    // FIX how do I map back to the right resource?
    for (ICompilationUnit cu : cus) {
      try {
        copyAsResource(tempDir, cu.getCorrespondingResource());
      } catch (JavaModelException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Create a patch by comparing the copy in the temp directory to the resource
   * 
   * @return The relative path to the patch
   */
  private String createPatch(IResource r) {
    final File cached = computeCopyFile(tempDir, r);
    final File changed = r.getLocation().toFile();
    List<String> original = fileToLines(cached);
    List<String> revised = fileToLines(changed);

    // Compute diff. Get the Patch object. Patch is the container for
    // computed deltas.
    final Patch patch = DiffUtils.diff(original, revised);
    for (Delta delta : patch.getDeltas()) {
      System.out.println(delta);
    }
    final List<String> diff = DiffUtils.generateUnifiedDiff(r.getName(), r.getName(), original, patch, 5);
    String patchName = computePatchName(r);
    final File diffFile = new File(scriptResourcesDir, patchName);
    try {
      linesToFile(diff, diffFile);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return patchName;
  }

  /**
   * @return A filename relative to the workspace
   */
  private String computePatchName(IResource r) {
    final String base = r.getFullPath() + "." + getId() + ".patch";
    File f = new File(scriptResourcesDir, base);
    String name = base;
    int i = 0;
    // Find a unique name
    while (f.exists()) {
      name = base + i;
      f = new File(scriptResourcesDir, name);
      i++;
    }
    return name;
  }

  private static void linesToFile(List<String> lines, File f) throws IOException {
    f.getParentFile().mkdirs();

    FileWriter fw = new FileWriter(f);
    PrintWriter pw = new PrintWriter(fw);
    for (String line : lines) {
      pw.println(line);
    }
    pw.close();
  }

  // Helper method for get the file content
  private static List<String> fileToLines(File f) {
    List<String> lines = new LinkedList<String>();
    String line = "";
    try {
      BufferedReader in = null;
      try {
    	  in = new BufferedReader(new FileReader(f));
    	  while ((line = in.readLine()) != null) {
    		  lines.add(line);
    	  }       
      } finally {
    	  if (in != null) {
    		  in.close();
    	  }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

  private String computePrefix() {
    final String temp = XUtil.recordScript();
    final int slash = temp.indexOf('/');
    final String prefix;
    if (slash < 0) {
      prefix = '/' + temp + '/' + "script";
    } else {
      prefix = '/' + temp;
    }
    return prefix;
  }

  private void scriptChanges(List<Pair<IResource, Integer>> resources) {
    // Lines queued to properly compute the number that need to be
    // atomically built
    final List<String> queue = new ArrayList<String>();
    for (Pair<IResource, Integer> p : resources) {
      final IResource r = p.first();
      final String rName = r.getName();
      if (r.getType() != IResource.FILE) {
        System.out.println("Ignoring " + rName);
        continue;
      } else if (!r.getName().endsWith(".java")) {
        System.out.println("Ignoring non-Java file: " + rName);
        continue;
      }
      final String prefix = computePrefix();
      final String path = r.getFullPath().toString();
      switch (p.second()) {
      case IResourceDelta.ADDED:
        String name = copyAsResource(scriptResourcesDir, r);
        copyAsResource(tempDir, r);

        // Use the directory that we'll be importing into
        final int lastSlash = path.lastIndexOf('/');
        final String dest = lastSlash < 0 ? path : path.substring(0, lastSlash);
        queue.add(ScriptCommands.IMPORT + ' ' + dest + ' ' + prefix + name);
        break;
      case IResourceDelta.CHANGED:
        String patch = createPatch(r);
        copyAsResource(tempDir, r, true); // Update the patched file
        queue.add(ScriptCommands.PATCH_FILE + ' ' + path + ' ' + prefix + patch);
        break;
      case IResourceDelta.REMOVED:
        queue.add(ScriptCommands.DELETE_FILE + ' ' + path);
        break;
      default:
        System.out.println("Couldn't handle flag: " + p.second());
      }
    }
    /*
     * No longer needed with manual scans if (queue.size() > 1) {
     * printToScript("unset " + ScriptCommands.AUTO_BUILD); }
     */
    for (String line : queue) {
      printToScript(line);
    }
    /*
     * if (queue.size() > 1) { printToScript("set " +
     * ScriptCommands.AUTO_BUILD); }
     */
  }

  public void recordProjectAction(String action, IProject p) {
    if (script != null) {
      printToScript(action + ' ' + p.getName());
    }
  }

  public void recordProjectAction(String action, Iterable<IProject> projs) {
    if (script != null) {
      if (ignoreNextCleanup && ScriptCommands.CLEANUP_DROPS_FIRST.equals(action)) {
        System.out.println("Skipping first cleanup");
        ignoreNextCleanup = false;
        return;
      } else if (projs == null) {
        // Shortcut if there are no args
        printToScript(action);
        return;
      }
      final StringBuilder sb = new StringBuilder(action);
      boolean first = true;
      sb.append(' ');
      /*
       * if (projs == null) { projs = JDTUtility.getProjects(); }
       */
      for (IProject p : projs) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(p.getName());
      }
      printToScript(sb.toString());
    }
  }

  // Was public void recordViewUpdate() {
  @Override
  public void currentScanChanged(JSureScan scan) {
    if (script != null) {
      // Export results
      final String prefix = "expectedResults" + getId();
      try {
        final String path = computePrefix();
        final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
        if (info == null) {
        	return;
        }
        final File results = info.getJSureRun().getResultsFile();        
        final String name = prefix + 
        		(results.getName().endsWith(".gz") ? RegressionUtility.JSURE_SNAPSHOT_SUFFIX+".gz" : RegressionUtility.JSURE_SNAPSHOT_SUFFIX);
        final File location = new File(scriptResourcesDir, name);
        FileUtility.copy(results, location);

        printToScript(ScriptCommands.COMPARE_RESULTS + " workspace " + path + '/' + name + " " + path + "/../" + prefix
            + RegressionUtility.JSURE_SNAPSHOT_DIFF_SUFFIX);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static final Comparator<? super File> fileComparator = new Comparator<File>() {
    @Override
    public int compare(File o1, File o2) {
      return (int) (o1.lastModified() - o2.lastModified());
    }
  };

  public void stopScripting() {
    if (info != null) {
      if (script != null) {
        script.close();
        System.out.println("Stopped scripting.");
      }
      try {
        final File projDir = scriptResourcesDir.getParentFile();
        final File baseDir = projDir.getParentFile();
        // Updating a previously created script
        if (XUtil.updateScript() != null) {
          // Only add the sea.xml files
          File[] files = scriptResourcesDir.listFiles(updateFilter);
          Arrays.sort(files, fileComparator);
          for (File f : files) {
            Long oldLength = deleted.remove(f.getName());
            if (oldLength == null) {
              throw new IllegalStateException("Created an extra file: " + f.getName());
            }
            System.out.println("\nUpdated " + f.getName() + ": \t" + oldLength + " -> " + f.length());
            try {
              final ISeaDiff d = SeaSnapshotDiff.diff(UninterestingPackageFilterUtility.UNINTERESTING_PACKAGE_FILTER, new File(
                  deletedDir, f.getName()), f);
              if (d.isEmpty()) {
                System.out.println("\tNo differences.");
              }
            } catch (Exception e) {
              System.out.println("Couldn't diff " + f);
              e.printStackTrace();
            }
            info.zipFile(baseDir, f);
          }
          for (Map.Entry<String, Long> e : deleted.entrySet()) {
            System.out.println("NOT updated " + e.getKey());
          }
        } else {
          info.zipDir(baseDir, scriptResourcesDir);
          info.zipFile(baseDir, new File(projDir, ScriptCommands.NAME));
        }
        final File settings = new File(projDir, ScriptCommands.ANALYSIS_SETTINGS);
        if (!settings.exists()) {
          ((JavacEclipse) IDE.getInstance()).writePrefsToXML(settings);
          //DoubleChecker.getDefault().writePrefsToXML(settings);
          info.zipFile(baseDir, settings);
        }
        final File props = new File(projDir, ScriptCommands.TEST_PROPERTIES);
        if (!props.exists()) {
          // Find out which settings to include
          final StringBuilder sb = new StringBuilder("moreVMargs=");
          boolean first = true;
          for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
            final String key = e.getKey().toString();
            if (key.startsWith("SureLogic") && !(key.endsWith("Script"))) {
              System.out.println(key + " = " + e.getValue());
              if (first) {
                first = false;
              } else {
                sb.append(' ');
              }
              sb.append("-D").append(key).append('=').append(e.getValue());
            }
          }
          System.out.println(sb);
          PrintWriter pw = new PrintWriter(props);
          /*
           * pw.print("moreVMargs=-Dfluid.ir.versioning=Versioning.Off " );
           * pw.print(
           * "-Ddoublechecker.useSuperRoots=SuperRoots.Off -Ddc.show.private=true "
           * ); pw.print(
           * "-Dsurelogic.useNewParser=Parser.On -Dxml.useNewParser=Parser.On "
           * ); pw.println("-Drules.useNewScopedPromises=Promise.On");
           */
          pw.println(sb);
          pw.close();
          info.zipFile(baseDir, props);
        }
        info.close();
        System.out.println("Finished script: "+info.getFile());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static final TempFileFilter filter = new TempFileFilter(SCRIPT_TEMP, ".dir");
  private static final TempFileFilter scriptFilter = new TempFileFilter(SCRIPT_TEMP, ".txt");
  private static final FilenameFilter updateFilter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(RegressionUtility.JSURE_SNAPSHOT_SUFFIX);
    }
  };
  private static final TempFileFilter deletedDirFilter = new TempFileFilter("deletedFromScript", ".dir");

  private static final JavacDriver prototype = new JavacDriver();
  static {
    if (prototype.updateScriptJob != null) {
      prototype.updateScript();
    }
    if (prototype.script != null) {
      ResourcesPlugin.getWorkspace().addResourceChangeListener(prototype, IResourceChangeEvent.POST_CHANGE);
      // This only worked in the old world, when I waited for a build
      // IResourceChangeEvent.PRE_BUILD);
      // IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE
    }
  }

  public static JavacDriver getInstance() {
    return prototype;
  }

  volatile SLProgressMonitor lastMonitor = null;

  class JavacProjectInfo extends ProjectInfo<JavacProject> {

	JavacProjectInfo(IProject project, List<ICompilationUnit> cus) {
		super(project, cus);
	}
	 
	@Override
	protected void setProjectSpecificProperties(Config config) {
		// TODO obsolete?
		Properties props2 = JSureProperties.read(config.getLocation());
		if (props2 != null) {
			JSureProperties.handle(config.getProject(), props2);
		}
	}

	@Override
	protected ProjectInfo<JavacProject> getProjectInfo(IProject proj) {
		return JavacDriver.this.getProjectInfo(proj);
	}

	@Override
	protected void setDefaultJRE(String name) {
		JavacEclipse.getDefault().setPreference(IDEPreferences.DEFAULT_JRE, name);
	}
  }
  
  public void preBuild(final IProject p) {
    System.out.println("Pre-build for " + p);
    /*
     * if (building.isEmpty()) { // First project build, so populate with active
     * projects for(IJavaProject jp : JDTUtility.getJavaProjects()) { final
     * IProject proj = jp.getProject(); if (Nature.hasNature(proj)) {
     * building.add(proj); } } }
     */
  }

  @Override
  protected JavacProjectInfo finishRegisteringFullBuild(IProject project, List<Pair<IResource, Integer>> resources, List<ICompilationUnit> cus) {
	  if (script != null) {
		  cacheCompUnits(cus);
	  }
	  return new JavacProjectInfo(project, cus);
  }

  @Override
  protected void finishRegisteringIncrementalBuild(List<Pair<IResource, Integer>> resources, List<ICompilationUnit> cus) {
	  if (script != null) {
		  scriptChanges(resources);
	  }
  }
  
  @Override
  public void clearProjectInfo() {
	super.clearProjectInfo();
    ModuleRules.clearSettings();
    ModuleRules.clearAsSourcePatterns();
    ModuleRules.clearAsNeededPatterns();
  }

  @Override
  @SuppressWarnings({ "rawtypes" })
  public void doExplicitBuild(Map args, boolean ignoreNature) {
    JavacEclipse.initialize();
    if (script != null) {
      printToScript(ScriptCommands.RUN_JSURE);
    }
    super.doExplicitBuild(args, ignoreNature);
  }

  @Override
  public void configureBuild(File location, boolean isAuto /* IProject p */, boolean ignoreNature) {
    // System.out.println("Finished 'build' for "+p);
    /*
     * //ProjectDrop.ensureDrop(p.getName(), p); final ProjectInfo info =
     * projects.get(p); if (info == null) { return; // No info! }
     */
    /*
     * // Check if any projects are still building building.remove(p); if
     * (!building.isEmpty()) {
     * System.out.println("Still waiting for "+building); return; }
     */

    // TODO this needs to be run after ALL the info is collected
    JavacEclipse.initialize();
    if (!XUtil.testing) {
      SLLogger.getLogger().fine("Configuring analyses for build");
      ((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs();
    }
    super.configureBuild(location, isAuto, ignoreNature);
  }

  @Override
  protected File prepForScan(Projects newProjects, SLProgressMonitor monitor, boolean useSeparateJVM) throws Exception {
	  if (!XUtil.testing) {
		  System.out.println("Configuring analyses for doBuild");
		  ((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs();
	  }
	  return super.prepForScan(newProjects, monitor, useSeparateJVM);
  }
  
  @Override
  protected File getDataDirectory() {
	  return JSurePreferencesUtility.getJSureDataDirectory();
  }
  
  @Override
  protected void markAsRunning(File runDir) {
      RemoteJSureRun.markAsRunning(runDir);
  }
  
  @Override
  protected AnalysisJob makeAnalysisJob(Projects newProjects, File target, File zips, boolean useSeparateJVM) {
	  Projects oldProjects = null; // See code in prepForScan()
      /*
       * TODO JSureHistoricalSourceView.setLastRun(newProjects, new
       * ISourceZipFileHandles() { public Iterable<File> getSourceZips() {
       * return Arrays.asList(zips.listFiles()); } });
       */

      if (!clearBeforeAnalysis && oldProjects != null) {
        findModifiedFiles(newProjects, oldProjects);
      }
      // TODO create constants?

      return new AnalysisJob(oldProjects, newProjects, target, zips, useSeparateJVM);
  }
  
  @Override 
  protected void scheduleScanForExecution(Projects newProjects, SLJob copy) throws Exception {
	  if (ScriptCommands.USE_EXPECT && script != null) {
		  recordFilesToBuild(newProjects);
	  }
	  if (XUtil.testing) {
		  final File expected = (File) newProjects.getArg(ScriptCommands.EXPECT_BUILD);
		  if (expected != null && expected.exists()) {
			  checkForExpectedSourceFiles(newProjects, expected);
		  }
		  copy.run(new NullSLProgressMonitor());
	  } else {
		  super.scheduleScanForExecution(newProjects, copy);
	  }
  }

  private void findModifiedFiles(final Projects newProjects, Projects oldProjects) {
    // System.out.println("Checking for files modified after "+oldProjects.getDate());
    final Map<IJavaProject, Date> times = new HashMap<IJavaProject, Date>();
    for (JavacProject jp : newProjects) {
      // Check if we used it last time
      if (oldProjects.get(jp.getName()) != null) {
        // System.out.println("Checking for "+jp.getName());
        if (jp.getName().contains("/")) {
          continue;
        }
        IJavaProject ijp = JDTUtility.getJavaProject(jp.getName());
        if (ijp != null) {
          times.put(ijp, oldProjects.getDate());
        }
      }
    }
    if (times.size() == 0) {
      return;
    }

    final MultiMap<String, ICompilationUnit> byProj = new MultiHashMap<String, ICompilationUnit>();
    for (ICompilationUnit icu : JDTUtility.modifiedCompUnits(times, new NullProgressMonitor())) {
      byProj.put(icu.getJavaProject().getElementName(), icu);
    }
    for (IJavaProject ijp : times.keySet()) {
      final JavacProject jp = newProjects.get(ijp.getElementName());
      if (jp != null) {
        final Collection<ICompilationUnit> cus = byProj.get(ijp.getElementName());
        final Config c = jp.getConfig();
        if (cus != null && cus.size() > 0) {
          System.out.println(ijp.getElementName() + " has " + cus.size() + " modified files");
          try {
            c.intersectFiles(ProjectInfo.convertCompUnits(c, cus));
          } catch (JavaModelException e1) {
            // Suppressed, since it's an optimization
          }
        } else {
          // No changed files, so clear it out
          c.intersectFiles(Collections.<JavaSourceFile> emptyList());
        }
      }
    }
  }

  /**
   * Wait for a normal Eclipse build
   */
  public static void waitForBuild() {
    waitForBuild(true);
    waitForBuild(false);
  }

  class AnalysisJob extends AbstractAnalysisJob<Projects> {
	  private final Projects oldProjects;
	  
	  AnalysisJob(Projects oldProjects, Projects projects, File target, File zips, boolean useSeparateJVM) {
		  super(projects, target, zips, useSeparateJVM);
		  if (useSeparateJVM) {
			  this.oldProjects = null;
		  } else {
			  this.oldProjects = oldProjects;
		  }
	  }

	  @Override
	  public SLStatus run(SLProgressMonitor monitor) {
		  lastMonitor = monitor;
		  projects.setMonitor(monitor);
		  /*
		   * if (XUtil.testingWorkspace) {
		   * System.out.println("Clearing state before running analysis");
		   * ClearProjectListener.clearJSureState(); }
		   */
		  return super.run(monitor);
	  }
		  
	  @Override
	  protected void init(SLProgressMonitor monitor) throws IOException {
		  JavacEclipse.initialize();
		  System.out.println("JSure data dir  = " + IDE.getInstance().getStringPreference(IDEPreferences.JSURE_DATA_DIRECTORY));
		  System.out.println("XML diff dir    = " + IDE.getInstance().getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY));
		  NotificationHub.notifyAnalysisStarting();
		  JavacEclipse.getDefault().savePreferences(projects.getRunDir());
	  }
	  
	  @Override
	  protected LocalJSureJob makeLocalJob() throws Exception {
		  // Normally done by Javac, but needs to be repeated locally
		  final boolean noConflict;
		  if (oldProjects != null) {
			  noConflict = !projects.conflictsWith(oldProjects);
			  if (noConflict) {
				  projects.init(oldProjects);
			  } else {
				  System.out.println("Detected a conflict between projects");
			  }
		  } else {
			  noConflict = true;
		  }
		  
		  System.out.println("run = " + projects.getRun());
		  final String msg = "Running JSure for " + projects.getLabel();
		  ILocalConfig cfg = new EclipseLocalConfig(JSurePreferencesUtility.getMaxMemorySize(), projects.getRunDir());
		  return LocalJSureJob.factory.newJob(msg, 100, cfg);
	  }

	  @Override
	  protected boolean analyzeInVM() throws Exception {
		  File tmpLocation;
		  if (clearBeforeAnalysis || oldProjects == null) {
			  // ClearProjectListener.clearJSureState();

			  tmpLocation = Util.openFiles(projects, true);
		  } else {
			  tmpLocation = Util.openFiles(oldProjects, projects, true);
		  }
		  boolean ok = tmpLocation != null;
		  // Persist the Sea
		  //final File tmpLocation = RemoteJSureRun.snapshot(System.out, projects.getLabel(), projects.getRunDir());
		  /*
    SeaStats.createSummaryZip(new File(projects.getRunDir(), RemoteJSureRun.SUMMARIES_ZIP), Sea.getDefault().getDrops(),
        SeaStats.splitByProject, SeaStats.STANDARD_COUNTERS);
    System.out.println("Finished " + RemoteJSureRun.SUMMARIES_ZIP);
		   */

		  // Create empty files
		  /*
    final File log = new File(projects.getRunDir(), RemoteJSureRun.LOG_NAME);
    log.createNewFile();
		   */
		  ClearStateUtility.clearAllState();
		  RemoteJSureRun.renameToFinalName(System.out, projects.getRunDir(), tmpLocation);
		  return ok;
	  }
	    
	  @Override
	  protected void handleSuccess() {
		  final File runDir = projects.getRunDir();
		  // Unneeded after running the scan
		  FileUtility.recursiveDelete(new File(runDir, PersistenceConstants.SRCS_DIR), false);

		  JSureDataDirHub.getInstance().scanDirectoryAdded(projects.getRunDir());
	  }

	  @Override 
	  protected void handleFailure() {
		  NotificationHub.notifyAnalysisPostponed();
	  }

	  @Override
	  protected void handleCrash(SLProgressMonitor monitor, SLStatus status) {
		  if (monitor.isCanceled()) {				
			  return; // Ignore this 
		  }
		  if (XUtil.testing && status.getException() != null) { 			  
			  throw new RuntimeException(status.getException());
		  }
		  /*
		   * Collect information and report this scan crash to SureLogic.
		   */
		  final File rollup = collectCrashFiles(projects);
		  if (XUtil.testing) {
			  if (status.getException() != null) {
				  status.getException().printStackTrace();
				  throw new RuntimeException(status.getException());
			  } else {
				  System.err.println("CRASH: " + status.getMessage());
				  throw new RuntimeException(status.getMessage());
			  }
		  } else {
			  JSureScanCrashReport.getInstance().getReporter().reportScanCrash(status, rollup);
		  }
		  /*
		   * Because we already opened a dialog above about the crash, log it and
		   * bail out of the job.
		   */
		  status.logTo(SLLogger.getLogger());
	  }

    protected void endAnalysis(SLProgressMonitor monitor) {
    	if (lastMonitor == monitor) {
			lastMonitor = null;
		}
    }
    
    protected void finish(SLProgressMonitor monitor) {
		NotificationHub.notifyAnalysisCompleted();
		// recordViewUpdate();

		// Cleared here after notifications are processed
		// to prevent redoing some (binder) work
		IDE.getInstance().clearCaches();
    }
  }

  private static File collectCrashFiles(Projects projects) {
    final File crash = new File(projects.getRunDir(), CRASH_FILES);
    try {
      PromisesXMLArchiver out = new PromisesXMLArchiver(crash);
      try {
        out.archive(ScanProperty.SCAN_PROPERTIES, 
        		    new File(projects.getRunDir(), ScanProperty.SCAN_PROPERTIES));
    	out.archive(Javac.JAVAC_PROPS, new File(projects.getRunDir(), Javac.JAVAC_PROPS));
    	  
        // Get project-specific config
        for (String name : projects.getProjectNames()) {
          IProject proj = EclipseUtility.getProject(name);
          if (proj == null) {
            out.outputWarning("Project does not exist: " + name);
            continue;
          }
          IPath projLocation = proj.getLocation();
          if (projLocation != null) {
            File projFile = projLocation.toFile();
            if (projFile != null && projFile.isDirectory()) {
              for (String config : AbstractJavaZip.CONFIG_FILES) {
                out.archive(projFile.getName() + '/' + config, new File(projFile, config));
              }
            } else {
              out.outputWarning("File could not be created for project location: " + projLocation);
              continue;
            }
          } else {
            out.outputWarning("Project location could not be retrieved: " + name);
            continue;
          }
        }
        out.archive(PersistenceConstants.PROJECTS_XML, new File(projects.getRunDir(), PersistenceConstants.PROJECTS_XML));
        out.archive(AbstractRemoteSLJob.LOG_NAME, new File(projects.getRunDir(), AbstractRemoteSLJob.LOG_NAME));

        final File libDir = JSurePreferencesUtility.getJSureXMLDirectory();
        FileUtility.recursiveIterate(out, libDir);
      } finally {
        out.close();
      }
    } catch (IOException e) {
      // Couldn't create the new file for some reason
      return new File(projects.getRunDir(), AbstractRemoteSLJob.LOG_NAME);
    }
    return crash;
  }

  private static class PromisesXMLArchiver extends TextArchiver {
    public PromisesXMLArchiver(File target) throws IOException {
      super(target);
    }

    @Override
    public boolean accept(File pathname) {
      return TestXMLParserConstants.XML_FILTER.accept(pathname);
    }
  }

  int id = 0;

  int getId() {
    int rv = id;
    id++;
    return rv;
  }

  private void recordFilesToBuild(Projects p) throws FileNotFoundException {
    final String name = "expectedBuild" + getId() + ".txt";
    final File file = new File(scriptResourcesDir, name);

    final PrintWriter pw = new PrintWriter(file);
    for (Config c : p.getConfigs()) {
      for (JavaSourceFile f : c.getFiles()) {
        pw.println(f.relativePath);
      }
    }
    pw.close();
    printToScript(ScriptCommands.EXPECT_BUILD_FIRST + ' ' + computePrefix() + '/' + name);

    recordFilesToAnalyze(p);
  }

  private void recordFilesToAnalyze(Projects p) throws FileNotFoundException {
    final String name = "expectedAnalysis" + getId() + ".txt";
    final File file = new File(scriptResourcesDir, name);
    p.setArg(Util.RECORD_ANALYSIS, file);
    printToScript(ScriptCommands.EXPECT_ANALYSIS_FIRST + ' ' + computePrefix() + '/' + name);
  }

  private void checkForExpectedSourceFiles(Projects p, File expected) throws IOException {
    System.out.println("Checking source files expected for build");
    final Set<String> cus = RegressionUtility.readLinesAsSet(expected);
    boolean somethingToBuild = false;
    for (Config c : p.getConfigs()) {
      for (JavaSourceFile f : c.getFiles()) {
        if (!cus.remove(f.relativePath)) {
          throw new IllegalStateException("Building extra file: " + f.relativePath);
        } else {
          System.out.println("Correctly building: " + f.relativePath);
        }
        somethingToBuild = true;
      }
    }
    if (!cus.isEmpty()) {
      if (somethingToBuild) {
        throw new IllegalStateException("File not built: " + cus.iterator().next());
      } else {
        throw new IllegalStateException("No files built: " + cus.iterator().next());
      }
    }
  }

  // Somehow call scriptChanges
  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (script != null) {
      ChangeCollector visitor = new ChangeCollector();
      try {
        event.getDelta().accept(visitor);
        scriptChanges(visitor.getChanges());
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }

    if (event.getResource() == null) {
      for (IResourceDelta delta : event.getDelta().getAffectedChildren()) {
        changed(delta);
      }
      return;
    }
    if (!(event.getResource() instanceof IProject)) {
      // Gets changes to the root /
      System.out.println("Ignoring change1 to " + event.getResource());
      return;
    }
    switch (event.getType()) {
    case IResourceChangeEvent.PRE_DELETE:
      // Handled by removal
      // System.out.println("Ignoring deletion of project "+event.getResource().getName());
      break;
    case IResourceChangeEvent.PRE_CLOSE:
      /*
       * Handled below if (script != null) {
       * printToScript(ScriptCommands.CLOSE_PROJECT
       * +' '+event.getResource().getName()); }
       */
      break;
    default:
      System.out.println("Ignoring change2 to " + event.getResource().getName());
      return;
    case IResourceChangeEvent.PRE_BUILD:
      changed(event.getDelta());
    }
  }

  private void changed(IResourceDelta delta) {
    if (!(delta.getResource() instanceof IProject)) {
      System.out.println("Ignoring change4 to " + delta.getResource());
      return;
    }
    switch (delta.getKind()) {
    case IResourceDelta.ADDED:
      System.out.println("Handling addition as a new project " + delta.getResource());
      if (script != null) {
        printToScript(ScriptCommands.CREATE_PROJECT + ' ' + delta.getResource().getName());
      }
      return;
    case IResourceDelta.REMOVED:
      System.out.println("Handling removal of project " + delta.getResource());
      if (script != null) {
        printToScript(ScriptCommands.DELETE_PROJECT + ' ' + delta.getResource().getName());
      }
      return;
    case IResourceDelta.CHANGED:
      if (delta.getFlags() != IResourceDelta.OPEN) {
        System.out.println("Ignoring change5 to project " + delta.getResource() + ": " + delta.getFlags());
        /*
         * for(IResourceDelta d : delta.getAffectedChildren()) { changed(d); }
         */
        return;
      }
      final IProject p = (IProject) delta.getResource();
      if (p.isOpen()) {
        System.out.println("Handling opening project " + delta.getResource());
        if (script != null) {
          printToScript(ScriptCommands.OPEN_PROJECT + ' ' + delta.getResource().getName());
        }
      } else {
        System.out.println("Handling closing project " + delta.getResource());
        if (script != null) {
          printToScript(ScriptCommands.CLOSE_PROJECT + ' ' + delta.getResource().getName());
        }
      }
      return;
    default:
      System.out.println("Ignoring change3 to " + delta.getResource());
      return;
    }
  }

  static class ChangeCollector implements IResourceDeltaVisitor {
    private final List<Pair<IResource, Integer>> changes = new ArrayList<Pair<IResource, Integer>>();

    public List<Pair<IResource, Integer>> getChanges() {
      return changes;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
      if (delta.getResource().getType() != IResource.FILE) {
        return true;
      }
      changes.add(new Pair<IResource, Integer>(delta.getResource(), delta.getKind()));
      return false;
    }
  }
}
