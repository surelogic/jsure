package com.surelogic.jsure.core.driver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.JSureProperties;
import com.surelogic.annotation.rules.ModuleRules;
import com.surelogic.common.FileUtility;
import com.surelogic.common.FileUtility.TempFileFilter;
import com.surelogic.common.FileUtility.UnzipCallback;
import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.PeriodicUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.ZipInfo;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.SourceZip;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.remote.TestCode;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.IClassPathEntry;
import com.surelogic.fluid.javac.JarEntry;
import com.surelogic.fluid.javac.JavaSourceFile;
import com.surelogic.fluid.javac.JavacProject;
import com.surelogic.fluid.javac.JavacTypeEnvironment;
import com.surelogic.fluid.javac.Projects;
import com.surelogic.fluid.javac.PromiseMatcher;
import com.surelogic.fluid.javac.Util;
import com.surelogic.fluid.javac.jobs.ILocalJSureConfig;
import com.surelogic.fluid.javac.jobs.LocalJSureJob;
import com.surelogic.jsure.core.listeners.ClearProjectListener;
import com.surelogic.jsure.core.listeners.NotificationHub;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scripting.ExportResults;
import com.surelogic.jsure.core.scripting.ICommandContext;
import com.surelogic.jsure.core.scripting.NullCommand;
import com.surelogic.jsure.core.scripting.ScriptCommands;
import com.surelogic.jsure.core.scripting.ScriptReader;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;
import edu.cmu.cs.fluid.sea.xml.SeaSummary.Diff;
import edu.cmu.cs.fluid.util.Pair;

public class JavacDriver implements IResourceChangeListener {
	private static final String SCRIPT_TEMP = "scriptTemp";

	private static final Logger LOG = SLLogger
			.getLogger("analysis.JavacDriver");

	/**
	 * Clear all the JSure state before each build
	 */
	private static final boolean clearBeforeAnalysis = false;

	private static final boolean useSourceZipsDirectly = false;

	enum BuildState {
		// Null means no build right now
		WAITING, BUILDING
	}

	enum RebuildState {
		// Null means no need to rebuild
		AUTO, FULL
	}

	/**
	 * If true, create common projects for shared jars Otherwise, jars in
	 * different are treated as if they're completely unique
	 * 
	 * Creating separate projects for shared jars doesn't work, due to
	 * dependencies on other jars, esp. the JRE
	 */
	private static final boolean shareCommonJars = false;

	// private final List<IProject> building = new ArrayList<IProject>();
	private final Map<String, Object> args = new HashMap<String, Object>();
	private final Map<IProject, ProjectInfo> projects = new HashMap<IProject, ProjectInfo>();
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

	/**
	 * State that needs to be atomically modified
	 */
	private BuildState buildState = null;
	private RebuildState rebuildQueue = null;

	synchronized RebuildState makeTransition(BuildState before,
			BuildState after, RebuildState rebuild) {
		if (buildState != before) {
			throw new IllegalStateException("Build state isn't " + before
					+ ": " + buildState);
		}
		buildState = after;
		if (after == null) {
			// Wake up anyone who's waiting for this
			notifyAll();
		}
		try {
			return rebuildQueue;
		} finally {
			rebuildQueue = rebuild;
		}
	}

	public synchronized void waitForJSureBuild() {
		if (buildState != null) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		// Otherwise, just keep going
	}

	private JavacDriver() {
		PeriodicUtility.addHandler(new Runnable() {
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
						throw new IllegalStateException(
								"Unable to delete project: " + proj);
					}
				}
				proj.mkdirs();

				// Unzip into the project
				FileUtility.unzipFile(archive, proj);

				// Make a copy of the script to use while updating
				FileUtility.deleteTempFiles(scriptFilter);
				scriptBeingUpdated = scriptFilter.createTempFile();
				FileUtility.copy(new File(proj, ScriptCommands.NAME),
						scriptBeingUpdated);

				// Make a directory to keep the "deleted" files
				FileUtility.deleteTempFiles(deletedDirFilter);
				deletedDir = deletedDirFilter.createTempFolder();

				// Import the project into the workspace
				EclipseUtility.importProject(proj);
			} catch (Exception e) {
				throw new IllegalStateException(
						"Could not create/import project", e);
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
				File testProps = new File(workspace, proj + File.separatorChar
						+ ScriptCommands.TEST_PROPERTIES);
				if (testProps.exists()) {
					// This will be re-created later from the actual settings
					// used
					testProps.delete();
				}
			}
			PrintStream out = null;
			ZipInfo zipInfo = null;
			File tmp = null;
			final File scriptF = new File(workspace, proj + File.separatorChar
					+ ScriptCommands.NAME);
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
				zipInfo = FileUtility.zipDirAndMore(new File(workspace, proj),
						zip);
				if (update == null) {
					out = new PrintStream(scriptF);
				}
				FileUtility.deleteTempFiles(filter);
				tmp = filter.createTempFolder();
			} catch (IOException e) {
				e.printStackTrace();
			}
			tempDir = tmp;
			script = (tmp == null) ? null : out;
			info = zipInfo;

			if (scriptBeingUpdated != null) {
				updateScriptJob = new AbstractSLJob("Updating script") {
					public SLStatus run(SLProgressMonitor monitor) {
						try {
							// TODO needs to run after the FTA auto-build
							final UpdateScriptReader r = new UpdateScriptReader(
									temp);
							return r.execute(scriptBeingUpdated) ? SLStatus.OK_STATUS
									: SLStatus.CANCEL_STATUS;
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

	public void updateScript() {
		if (updateScriptJob != null) {
			try {
				ResourcesPlugin.getWorkspace().build(
						IncrementalProjectBuilder.AUTO_BUILD, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// This is in a job to let it run after plugins are initialized
			EclipseJob.getInstance().schedule(
					new AbstractSLJob("Preparing to run update script job") {
						public SLStatus run(SLProgressMonitor monitor) {
							waitForJSureBuild();

							// This cannot lock the workspace, since it prevents
							// builds from happening
							System.out.println("Running update script job");
							EclipseJob.getInstance().schedule(updateScriptJob);
							return SLStatus.OK_STATUS;
						}
					});
		}
	}

	private static class UpdateScriptReader extends ScriptReader {
		public UpdateScriptReader(final String proj) {
			super(EclipseUtility.getProject(proj), true);
			// These should be the ops that we auto-inserted
			commands.put(ScriptCommands.EXPECT_BUILD, NullCommand.prototype);
			commands.put(ScriptCommands.EXPECT_ANALYSIS, NullCommand.prototype);
			commands.put(ScriptCommands.COMPARE_RESULTS, new ExportResults() {
				@Override
				public boolean execute(ICommandContext context,
						String... contents) throws Exception {
					// Reformat the contents for what it expects
					return super.execute(context, contents[0], proj,
							contents[2]);
				}
			});
		}

		@Override
		protected void finish() {
			// PlatformUI.getWorkbench().close();		
			String msg = "JSure is done re-running your script.  No actions are being recorded."
				+ "Please shutdown Eclipse to finish the archive.";
			//BalloonUtility.showMessage("Safe to shutdown", msg);					
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
				System.out
						.println("Already created a copy: " + r.getFullPath());
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
	 * Create a patch by comparing the copy in the temp directory to the
	 * resource
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
		final List<String> diff = DiffUtils.generateUnifiedDiff(r.getName(),
				r.getName(), original, patch, 5);
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

	private static void linesToFile(List<String> lines, File f)
			throws IOException {
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
			BufferedReader in = new BufferedReader(new FileReader(f));
			while ((line = in.readLine()) != null) {
				lines.add(line);
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
				final String dest = lastSlash < 0 ? path : path.substring(0,
						lastSlash);
				queue.add(ScriptCommands.IMPORT + ' ' + dest + ' ' + prefix
						+ name);
				break;
			case IResourceDelta.CHANGED:
				String patch = createPatch(r);
				copyAsResource(tempDir, r, true); // Update the patched file
				queue.add(ScriptCommands.PATCH_FILE + ' ' + path + ' ' + prefix
						+ patch);
				break;
			case IResourceDelta.REMOVED:
				queue.add(ScriptCommands.DELETE_FILE + ' ' + path);
				break;
			default:
				System.out.println("Couldn't handle flag: " + p.second());
			}
		}
		if (queue.size() > 1) {
			printToScript("unset " + ScriptCommands.AUTO_BUILD);
		}
		for (String line : queue) {
			printToScript(line);
		}
		if (queue.size() > 1) {
			printToScript("set " + ScriptCommands.AUTO_BUILD);
		}
	}

	public void recordProjectAction(String action, IProject p) {
		if (script != null) {
			printToScript(action + ' ' + p.getName());
		}
	}

	public void recordProjectAction(String action, Iterable<IProject> projs) {
		if (script != null) {
			if (ignoreNextCleanup
					&& ScriptCommands.CLEANUP_DROPS_FIRST.equals(action)) {
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

	public void recordViewUpdate() {
		if (script != null) {
			// Export results
			final String prefix = "expectedResults" + getId();
			final String name = prefix + SeaSnapshot.SUFFIX;
			final File location = new File(scriptResourcesDir, name);
			try {
				final String path = computePrefix();
				Sea.getDefault().updateConsistencyProof();
				SeaSummary.summarize("workspace", Sea.getDefault(), location);
				printToScript(ScriptCommands.COMPARE_RESULTS + " workspace "
						+ path + '/' + name + " " + path + "/../" + prefix
						+ RegressionUtility.JSURE_SNAPSHOT_DIFF_SUFFIX);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void stopScripting() {
		if (info != null) {
			if (script != null) {
				script.close();
			}
			try {
				final File baseDir = scriptResourcesDir.getParentFile();
				// Updating a previously created script
				if (XUtil.updateScript() != null) {
					// Only add the sea.xml files
					for (File f : scriptResourcesDir.listFiles(updateFilter)) {
						Long oldLength = deleted.remove(f.getName());
						if (oldLength == null) {
							throw new IllegalStateException(
									"Created an extra file: " + f.getName());
						}
						System.out.println("Updated " + f.getName() + ": \t"
								+ oldLength + " -> " + f.length());
						try {
							final Diff d = SeaSummary.diff(new File(deletedDir,
									f.getName()), f);
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
					info.zipFile(baseDir,
							new File(baseDir, ScriptCommands.NAME));
				}
				final File settings = new File(baseDir,
						ScriptCommands.ANALYSIS_SETTINGS);
				if (!settings.exists()) {
					DoubleChecker.getDefault().writePrefsToXML(settings);
					info.zipFile(baseDir, settings);
				}
				final File props = new File(baseDir,
						ScriptCommands.TEST_PROPERTIES);
				if (!props.exists()) {
					// Find out which settings to include
					final StringBuilder sb = new StringBuilder("moreVMargs=");
					boolean first = true;
					for (Map.Entry<Object, Object> e : System.getProperties()
							.entrySet()) {
						final String key = e.getKey().toString();
						if (key.startsWith("SureLogic")
								&& !(key.endsWith("Script"))) {
							System.out.println(key + " = " + e.getValue());
							if (first) {
								first = false;
							} else {
								sb.append(' ');
							}
							sb.append("-D").append(key).append('=')
									.append(e.getValue());
						}
					}
					System.out.println(sb);
					PrintWriter pw = new PrintWriter(props);
					/*
					 * pw.print("moreVMargs=-Dfluid.ir.versioning=Versioning.Off "
					 * ); pw.print(
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static final TempFileFilter filter = new TempFileFilter(
			SCRIPT_TEMP, ".dir");
	private static final TempFileFilter scriptFilter = new TempFileFilter(
			SCRIPT_TEMP, ".txt");
	private static final FilenameFilter updateFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(RegressionUtility.JSURE_SNAPSHOT_SUFFIX);
		}
	};
	private static final TempFileFilter deletedDirFilter = new TempFileFilter(
			"deletedFromScript", ".dir");

	private static final JavacDriver prototype = new JavacDriver();
	static {
		if (prototype.updateScriptJob != null) {
			prototype.updateScript();
		}
		if (prototype.script != null) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(prototype,
					IResourceChangeEvent.PRE_BUILD);
			// IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE
		}
	}

	public static JavacDriver getInstance() {
		return prototype;
	}

	volatile SLProgressMonitor lastMonitor = null;

	class ProjectInfo {
		final IProject project;
		final List<ICompilationUnit> allCompUnits;
		final Set<ICompilationUnit> cuDelta = new HashSet<ICompilationUnit>();
		final Set<IResource> removed = new HashSet<IResource>();
		/**
		 * All comp units includes delta?
		 */
		boolean updated = true;
		boolean active = true;

		ProjectInfo(IProject p, List<ICompilationUnit> cus) {
			project = p;
			allCompUnits = new ArrayList<ICompilationUnit>(cus);
		}

		void setActive(boolean value) {
			active = value;
		}

		boolean isActive() {
			return active;
		}

		boolean hasDeltas() {
			return !cuDelta.isEmpty();
		}

		void registerDelta(List<ICompilationUnit> cus) {
			if (!cus.isEmpty()) {
				cuDelta.addAll(cus);
				updated = false;
			}
		}

		void registerResourcesDelta(List<Pair<IResource, Integer>> resources) {
			for (Pair<IResource, Integer> p : resources) {
				if (p.second() == IResourceDelta.REMOVED
						&& p.first().getName().endsWith(".java")) {
					removed.add(p.first());
					updated = false;
				}
			}
		}

		private boolean needsUpdate() {
			return !updated && !cuDelta.isEmpty();
		}

		Iterable<ICompilationUnit> getAllCompUnits() {
			if (needsUpdate()) {
				update(allCompUnits, cuDelta, removed);
			}
			return allCompUnits;
		}

		Iterable<IResource> getRemovedResources() {
			return removed;
		}

		Iterable<ICompilationUnit> getDelta() {
			if (needsUpdate()) {
				Iterable<ICompilationUnit> result = new ArrayList<ICompilationUnit>(
						cuDelta);
				update(allCompUnits, cuDelta, removed);
				return result;
			}
			return allCompUnits;
		}

		/**
		 * Adds itself to projects to make sure that it's not created multiple
		 * times
		 */
		Config makeConfig(final Projects projects, boolean all)
				throws JavaModelException {
			final IJavaProject jp = JDTUtility
					.getJavaProject(project.getName());
			if (jp == null) {
				return null;
			}
			scanForJDK(projects, jp);

			final File location = EclipseUtility.resolveIPath(project
					.getLocation());
			Config config = new ZippedConfig(project.getName(), location, false);
			projects.add(config);
			setOptions(config);

			for (IResource res : getRemovedResources()) {
				final File f = res.getLocation().toFile();
				config.addRemovedFile(f);
			}
			for (JavaSourceFile jsf : convertCompUnits(config,
					all ? getAllCompUnits() : getDelta())) {
				config.addFile(jsf);
			}
			addDependencies(projects, config, project, false);
			return config;
		}

		private void setOptions(Config config) {
			IJavaProject jp = JDTUtility.getJavaProject(config.getProject());
			int version = JDTUtility.getMajorJavaSourceVersion(jp);
			config.setOption(Config.SOURCE_LEVEL, version);
			// System.out.println(config.getProject()+": set to level "+version);

			if (config.getLocation() != null) {
				// TODO Is this right for multi-project configurations?
				ModuleRules.clearSettings();
				ModuleRules.clearAsSourcePatterns();
				ModuleRules.clearAsNeededPatterns();

				final File properties = new File(config.getLocation(),
						JSureProperties.JSURE_PROPERTIES);
				if (properties.exists() && properties.isFile()) {
					final Properties props = new Properties();
					// props.put(PROJECT_KEY, p);
					try {
						InputStream is = new FileInputStream(properties);
						props.load(is);
						is.close();
					} catch (IOException e) {
						String msg = "Problem while loading "
								+ JSureProperties.JSURE_PROPERTIES + ": "
								+ e.getMessage();
						// reportProblem(msg, null);
						LOG.log(Level.SEVERE, msg, e);
					} finally {
						// Nothing to do
					}
					JSureProperties.handle(config.getProject(), props);
				}
			}
		}

		void addDependencies(Projects projects, Config config, IProject p,
				boolean addSource) throws JavaModelException {
			final IJavaProject jp = JDTUtility.getJavaProject(p.getName());
			// TODO what export rules?
			scanForJDK(projects, jp);

			for (IClasspathEntry cpe : jp.getResolvedClasspath(true)) {
				// TODO ignorable since they'll be handled by the compiler
				// cpe.getAccessRules();
				// cpe.combineAccessRules();

				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					if (addSource) {
						addSourceFiles(config, cpe);
					}
					config.addToClassPath(config);
					break;
				case IClasspathEntry.CPE_LIBRARY:
					// System.out.println("Adding "+cpe.getPath()+" for "+p.getName());
					final File f = EclipseUtility.resolveIPath(cpe.getPath());
					String mapped = projects.checkMapping(f);
					if (mapped != null) {
						JavacProject mappedProj = projects.get(mapped);
						if (mappedProj == null) {
							// Make project for jar
							mappedProj = makeJarConfig(projects, f, mapped);
						}
						config.addToClassPath(mappedProj.getConfig());
					} else {
						config.addJar(f, cpe.isExported());
					}
					break;
				case IClasspathEntry.CPE_PROJECT:
					final String projName = cpe.getPath().lastSegment();
					final JavacProject jcp = projects.get(projName);
					if (jcp != null) {
						// Already created
						config.addToClassPath(jcp.getConfig());
						break;
					}
					final IProject proj = ResourcesPlugin.getWorkspace()
							.getRoot().getProject(projName);
					final ProjectInfo info = JavacDriver.this.projects
							.get(proj);
					final Config dep;
					if (info != null) {
						final boolean hasDeltas = info.hasDeltas();
						dep = info.makeConfig(projects, hasDeltas);
					} else {
						final File location = EclipseUtility.resolveIPath(proj
								.getLocation());
						dep = new ZippedConfig(projName, location,
								cpe.isExported());
						projects.add(dep);
						setOptions(dep);
					}
					config.addToClassPath(dep);

					if (info == null) {
						addDependencies(projects, dep, proj, true);
					}
					break;
				default:
					System.out.println("Unexpected: " + cpe);
				}
			}
		}

		private void addSourceFiles(Config config, IClasspathEntry cpe) {
			// TODO handle multiple deltas?
			/*
			 * final File dir = EclipseUtility.resolveIPath(cpe.getPath());
			 * final File[] excludes = new
			 * File[cpe.getExclusionPatterns().length]; int i=0; for(IPath xp :
			 * cpe.getExclusionPatterns()) { excludes[i] =
			 * EclipseUtility.resolveIPath(xp); i++; }
			 */
			IContainer root = (IContainer) ResourcesPlugin.getWorkspace()
					.getRoot().findMember(cpe.getPath());
			final IResource[] excludes = new IResource[cpe
					.getExclusionPatterns().length];
			int i = 0;
			for (IPath xp : cpe.getExclusionPatterns()) {
				excludes[i] = root.findMember(xp);
				i++;
			}
			addJavaFiles(root, config, excludes);
		}

		private void addJavaFiles(IContainer dir, Config config,
				IResource... excluded) {
			try {
				addJavaFiles("", dir, config, excluded);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		private void addJavaFiles(String pkg, IContainer dir, Config config,
				IResource[] excluded) throws CoreException {
			for (IResource x : excluded) {
				if (dir.equals(x)) {
					return;
				}
			}
			if (dir == null || !dir.exists()) {
				return;
			}
			// System.out.println("Scanning "+dir.getAbsolutePath());
			boolean added = false;
			for (IResource r : dir.members()) {
				if (r instanceof IFile && r.getName().endsWith(".java")) {
					final ICompilationUnit icu = JavaCore
							.createCompilationUnitFrom((IFile) r);
					if ((icu != null)
							&& (icu.getJavaProject().isOnClasspath(icu))) {
						final File f = r.getLocation().toFile();
						// System.out.println("Found source file: "+f.getPath());
						String typeName = f.getName().substring(0,
								f.getName().length() - 5);
						String qname = pkg.length() == 0 ? typeName : pkg + '.'
								+ typeName;
						config.addFile(new JavaSourceFile(qname, f, f
								.getAbsolutePath()));
						if (!added) {
							added = true;
							/*
							 * if (debug) {
							 * System.out.println("Found java files in "+pkg); }
							 */
							config.addPackage(pkg);
						}
					}
				}
				if (r instanceof IContainer) {
					final String newPkg = pkg == "" ? r.getName() : pkg + '.'
							+ r.getName();
					addJavaFiles(newPkg, (IContainer) r, config, excluded);
				}
			}
		}

		/**
		 * Create a project/config for a shared jar
		 */
		private JavacProject makeJarConfig(Projects projects, File f,
				String name) {
			System.out.println("Creating shared jar: " + name);
			// Use its containing directory as a location
			final Config config = new Config(name, f.getParentFile(), true);
			config.addJar(f, true);
			return projects.add(config);
		}

		private void scanForJDK(Projects projects, IJavaProject jp)
				throws JavaModelException {
			if (jp == null) {
				return;
			}
			for (IClasspathEntry cpe : jp.getRawClasspath()) {
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
					final IClasspathContainer cc = JavaCore
							.getClasspathContainer(cpe.getPath(), jp);
					if (cc.getDescription().startsWith(
							JavacTypeEnvironment.JRE_LIBRARY)) { // HACK
						JavacProject jcp = findJRE(projects, cc);
						if (jcp == null) {
							projects.add(makeConfig(projects, cc));
						}
						return;
					}
				}
			}
		}

		private JavacProject findJRE(Projects projects,
				final IClasspathContainer cc) {
			final String name = cc.getPath().toPortableString();
			JavacProject jcp = projects.get(name);
			if (jcp == null) {
				// Not found by name, so check for existing JREs
				for (JavacProject p : projects) {
					if (p.getName().startsWith(JavacTypeEnvironment.JRE_NAME)
							&& compareJREs(p.getConfig(), cc)) {
						return p;
					}
				}
			}
			return jcp;
		}

		private boolean compareJREs(Config c, final IClasspathContainer cc) {
			final IClasspathEntry[] cpes = cc.getClasspathEntries();
			int i = 0;
			for (IClassPathEntry e : c.getClassPath()) {
				if (i >= cpes.length) {
					return false;
				}
				final IClasspathEntry cpe = cpes[i];
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
					final File f = EclipseUtility.resolveIPath(cpe.getPath());
					if (!(e instanceof JarEntry)) {
						return false;
					}
					JarEntry j = (JarEntry) e;
					if (!f.equals(j.getPath())) {
						return false;
					}
					break;
				default:
					return false;
				}
				i++;
			}
			return true;
		}

		/**
		 * Make a Config for the JRE
		 */
		private Config makeConfig(Projects projects,
				final IClasspathContainer cc) {
			final String name = cc.getPath().toPortableString();
			final Config config = new Config(name, null, true);
			for (IClasspathEntry cpe : cc.getClasspathEntries()) {
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
					final File f = EclipseUtility.resolveIPath(cpe.getPath());
					// System.out.println("Adding "+f+" for "+cc.getDescription());
					config.addJar(f, true);
					projects.mapToProject(f, name);
					break;
				default:
					throw new IllegalStateException("Got entryKind: "
							+ cpe.getEntryKind());
				}
			}
			JavacEclipse.getDefault().setPreference(IDEPreferences.DEFAULT_JRE,
					name);
			return config;
		}

		/**
		 * Either add/remove as needed
		 * 
		 * @param removed2
		 */
		void update(Collection<ICompilationUnit> all,
				Collection<ICompilationUnit> cus, Set<IResource> removed) {
			// Filter out removed files
			final Iterator<ICompilationUnit> it = all.iterator();
			while (it.hasNext()) {
				final ICompilationUnit cu = it.next();
				if (removed.contains(cu.getResource())) {
					it.remove();
				}
			}
			// Add in changed ones
			for (ICompilationUnit cu : cus) {
				// TODO use a Set instead?
				if (cu.getResource().exists()) {
					if (!all.contains(cu)) {
						all.add(cu);
						// System.out.println("Added:   "+cu.getHandleIdentifier());
					} else {
						// System.out.println("Exists:  "+cu.getHandleIdentifier());
					}
				} else {
					all.remove(cu);
					// System.out.println("Deleted: "+cu.getHandleIdentifier());
				}
			}
			updated = true;
		}
	}

	public void preBuild(final IProject p) {
		System.out.println("Pre-build for " + p);
		/*
		 * if (building.isEmpty()) { // First project build, so populate with
		 * active projects for(IJavaProject jp : JDTUtility.getJavaProjects()) {
		 * final IProject proj = jp.getProject(); if (Nature.hasNature(proj)) {
		 * building.add(proj); } } }
		 */
	}

	/**
	 * Register resources
	 */
	@SuppressWarnings("unchecked")
	public void registerBuild(IProject project, Map args,
			List<Pair<IResource, Integer>> resources, List<ICompilationUnit> cus) {
		final int k = getBuildKind(args);
		if (k == IncrementalProjectBuilder.CLEAN_BUILD
				|| k == IncrementalProjectBuilder.FULL_BUILD) {
			// TODO what about resources?
			projects.put(project, new ProjectInfo(project, cus));
			System.out.println("Got full build");
			if (script != null) {
				cacheCompUnits(cus);
			}
		} else {
			ProjectInfo info = projects.get(project);
			if (info == null) {
				throw new IllegalStateException("No full build before this?");
			}
			info.registerDelta(cus);
			info.registerResourcesDelta(resources);
			if (script != null) {
				scriptChanges(resources);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static int getBuildKind(Map args) {
		final String kind = (String) args.get(DriverConstants.BUILD_KIND);
		return Integer.parseInt(kind);
	}

	@SuppressWarnings("unchecked")
	public void configureBuild(Map args, boolean ignoreNature) {
		final int k = getBuildKind(args);
		configureBuild(
				EclipseUtility.getWorkspacePath(),
				(k & IncrementalProjectBuilder.AUTO_BUILD) == IncrementalProjectBuilder.AUTO_BUILD,
				ignoreNature);
	}

	public void configureBuild(File location, boolean isAuto /* IProject p */,
			boolean ignoreNature) {
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
			System.out.println("Configuring analyses for build");
			((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs();
		}
		ConfigureJob configure = new ConfigureJob("Configuring JSure build",
				location, isAuto, args, ignoreNature);
		synchronized (this) {
			if (buildState == null) {
				buildState = BuildState.WAITING;
			} else {
				// Build already going
				System.out.println("Already started ConfigureJob: "
						+ buildState);
				if (buildState == BuildState.BUILDING) {
					// We need to do another build, since there might be some
					// updates
					// after the currently running build
					if (isAuto) {
						if (rebuildQueue == null) {
							rebuildQueue = RebuildState.AUTO;
						}
						// Otherwise, it's already set correctly to AUTO or FULL
					} else {
						rebuildQueue = RebuildState.FULL;
					}
				} else {
					// Ok to ignore, because this will be handled by the
					// currently waiting build
				}
				return;
			}
			// Only if there's no build already
			System.out.println("Starting to configure JSure build");
			ProjectsDrop pd = ProjectsDrop.getDrop();
			if (pd != null) {
				for (JavacProject jp : ((Projects) pd.getIIRProjects())) {
					System.out.println("Deactivating " + jp);
					jp.deactivate();
				}
			}
			if (XUtil.testing) {
				configure.run(new NullSLProgressMonitor());
			} else {
				EclipseJob.getInstance().schedule(configure);
			}
		}
	}

	private void doBuild(final Projects newProjects, Map<String, Object> args,
			SLProgressMonitor monitor, boolean useSeparateJVM) {
		try {
			if (!XUtil.testing) {
				System.out.println("Configuring analyses for doBuild");
				((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs();
			}
			// final boolean hasDeltas = info.hasDeltas();
			makeProjects(newProjects);

			final File dataDir = JSurePreferencesUtility
					.getJSureDataDirectory();
			final Projects oldProjects = useSeparateJVM ? null : (Projects) ProjectsDrop.getProjects();
			if (oldProjects != null) {
				System.out.println("Old projects = "+oldProjects.getLabel());
			}
			newProjects.computeRun(dataDir, oldProjects);

			final File zips = new File(dataDir, newProjects.getRun() + "/zips");
			final File target = new File(dataDir, newProjects.getRun()
					+ "/srcs");
			target.mkdirs();

			/* TODO
			JSureHistoricalSourceView.setLastRun(newProjects,
					new ISourceZipFileHandles() {
						public Iterable<File> getSourceZips() {
							return Arrays.asList(zips.listFiles());
						}
					});
            */
			
			if (!clearBeforeAnalysis && oldProjects != null) {
				findModifiedFiles(newProjects, oldProjects);
			}
			// TODO create constants?

			AnalysisJob analysis = new AnalysisJob(oldProjects, newProjects,
					target, zips, useSeparateJVM);
			CopyJob copy = new CopyJob(newProjects, target, zips, analysis);
			if (script != null) {
				recordFilesToBuild(newProjects);
			}
			if (XUtil.testing) {
				final File expected = (File) args
						.get(ScriptCommands.EXPECT_BUILD);
				if (expected != null && expected.exists()) {
					checkForExpectedSourceFiles(newProjects, expected);
				}
				copy.run(new NullSLProgressMonitor());
			} else {
				EclipseJob.getInstance().scheduleWorkspace(copy);
			}
		} catch (Exception e) {
			System.err.println("Unable to make config for JSure");
			e.printStackTrace();
			if (XUtil.testing) {
				throw (RuntimeException) e;
			}
			return;
		}
	}

	private void findModifiedFiles(final Projects newProjects,
			Projects oldProjects) {
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
		for (ICompilationUnit icu : JDTUtility.modifiedCompUnits(times,
				new NullProgressMonitor())) {
			byProj.put(icu.getJavaProject().getElementName(), icu);
		}
		for (IJavaProject ijp : times.keySet()) {
			final JavacProject jp = newProjects.get(ijp.getElementName());
			if (jp != null) {
				final Collection<ICompilationUnit> cus = byProj.get(ijp
						.getElementName());
				final Config c = jp.getConfig();
				if (cus != null && cus.size() > 0) {
					System.out.println(ijp.getElementName() + " has "
							+ cus.size() + " modified files");
					try {
						c.intersectFiles(convertCompUnits(c, cus));
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

	private void findSharedJars(final Projects projects) {
		if (!shareCommonJars) {
			return;
		}
		/*
		 * try { final Map<File,File> shared = new HashMap<File, File>();
		 * for(IJavaProject p : JDTUtility.getJavaProjects()) {
		 * for(IClasspathEntry cpe : p.getResolvedClasspath(true)) { switch
		 * (cpe.getEntryKind()) { case IClasspathEntry.CPE_LIBRARY: final IPath
		 * path = cpe.getPath(); final File f =
		 * EclipseUtility.resolveIPath(path); if (shared.containsKey(f)) {
		 * //System.out.println("Repeated view: "+f); shared.put(f, f); } else
		 * if (f != null) { //System.out.println("First view:    "+f);
		 * shared.put(f, null); // Seen once } } } } // Create mappings for
		 * shared jars for(File path : shared.keySet()) { File f =
		 * shared.get(path); if (f != null) { projects.mapToProject(path,
		 * f.getAbsolutePath()); } else { // Ignore jars only seen once } } }
		 * catch (JavaModelException e) { return; }
		 */
	}

	// TODO how to set up for deltas?
	private Projects makeProjects(final Projects projects)
			throws JavaModelException {
		findSharedJars(projects);

		List<ProjectInfo> infos = new ArrayList<ProjectInfo>(
				this.projects.values());
		for (ProjectInfo info : infos) {
			if (!projects.contains(info.project.getName())) {
				if (info.isActive()) {
					Config c = info.makeConfig(projects, !info.hasDeltas());
					if (c == null) {
						continue;
					}
				} else {
					// Otherwise, it's inactive
					continue;
				}
			} else {
				// Already added as a dependency?
				info.setActive(true);
			}
			JavacProject proj = projects.get(info.project.getName());
			/*
			 * if (proj == null) { continue; }
			 */
			Config config = proj.getConfig();
			config.setAsSource();
		}

		// Remove inactive projects?
		for (ProjectInfo info : infos) {
			if (!info.isActive()) {
				this.projects.remove(info.project);
			}
		}
		return projects;

	}

	private Collection<JavaSourceFile> convertCompUnits(Config config,
			final Iterable<ICompilationUnit> cus) throws JavaModelException {
		List<JavaSourceFile> files = new ArrayList<JavaSourceFile>();
		for (ICompilationUnit icu : cus) {
			final IPath path = icu.getResource().getFullPath();
			final IPath loc = icu.getResource().getLocation();
			final File f = loc.toFile();
			String qname;
			if (f.exists()) {
				String pkg = null;
				for (IPackageDeclaration pd : icu.getPackageDeclarations()) {
					config.addPackage(pd.getElementName());
					pkg = pd.getElementName();
				}
				qname = icu.getElementName();
				if (qname.endsWith(".java")) {
					qname = qname.substring(0, qname.length() - 5);
				}
				if (pkg != null) {
					qname = pkg + '.' + qname;
				}
			} else { // Removed
				qname = f.getName();
			}
			files.add(new JavaSourceFile(qname, f, path.toPortableString()));
		}
		return files;
	}

	static class ZippedConfig extends Config {
		ZippedConfig(String name, File location, boolean isExported) {
			super(name, location, isExported);
		}

		@Override
		protected Config newConfig(String name, File location,
				boolean isExported) {
			return new ZippedConfig(name, location, isExported);
		}

		@Override
		public void zipSources(File zipDir) throws IOException {
			final IProject project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(getProject());
			final SourceZip srcZip = new SourceZip(project);
			File zipFile = new File(zipDir, project.getName() + ".zip");
			if (!zipFile.exists()) {
				zipFile.getParentFile().mkdirs();
				srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
			} else {
				// System.out.println("Already exists: "+zipFile);
			}
			super.zipSources(zipDir);
		}

		@Override
		public void copySources(File zipDir, File targetDir) throws IOException {
			final IProject project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(getProject());
			targetDir.mkdir();

			File projectDir = new File(targetDir, project.getName());
			File zipFile = new File(zipDir, project.getName() + ".zip");
			ZipFile zf = new ZipFile(zipFile);

			// Get class mapping (qname->zip path)
			Properties props = new Properties();
			ZipEntry mapping = zf.getEntry(AbstractJavaZip.CLASS_MAPPING);
			props.load(zf.getInputStream(mapping));

			// Reverse mapping
			final Map<String, List<String>> path2qnames = new HashMap<String, List<String>>();
			// int count = 0;
			for (Map.Entry<Object, Object> e : props.entrySet()) {
				String path = (String) e.getValue();
				List<String> l = path2qnames.get(path);
				if (l == null) {
					l = new ArrayList<String>();
					path2qnames.put(path, l);
				}
				l.add((String) e.getKey());
				// count++;
			}
			// System.out.println(getProject()+": class mapping "+count);
			/*
			 * for(JavaSourceFile f : getFiles()) {
			 * System.out.println(getProject()+": "+f.relativePath); }
			 */
			final List<JavaSourceFile> srcFiles = new ArrayList<JavaSourceFile>();
			final UnzipCallback callback = new UnzipCallback() {
				public void unzipped(ZipEntry ze, File f) {
					// Finish setting up srcFiles
					if (ze.getName().endsWith(".java")) {
						final List<String> names = path2qnames
								.get(ze.getName());
						if (names != null) {
							for (String name : names) {
								// System.out.println("Mapping "+name+" to "+f.getAbsolutePath());
								srcFiles.add(new JavaSourceFile(name.replace(
										'$', '.'), f, null));
							}
						} else if (ze.getName().endsWith("/package-info.java")) {
							System.out
									.println("What to do about package-info.java?");
						} else {
							System.err.println("Unable to get qname for "
									+ ze.getName());
						}
					} else {
						// System.out.println("Not a java file: "+ze.getName());
					}
				}
			};
			if (useSourceZipsDirectly) {
				// OK
				// jar:///C:/Documents%20and%20Settings/UncleBob/lib/vendorA.jar!com/vendora/LibraryClass.class
				final Enumeration<? extends ZipEntry> e = zf.entries();
				final String zipPath = zipFile.getAbsolutePath();
				while (e.hasMoreElements()) {
					ZipEntry ze = e.nextElement();
					File f = PromiseMatcher.makeZipReference(zipPath,
							ze.getName());
					// System.out.println("URI = "+f.toURI());
					callback.unzipped(ze, f);
				}
			} else {
				FileUtility.unzipFile(zf, projectDir, callback);
			}
			this.setFiles(srcFiles);
			super.copySources(zipDir, targetDir);
		}
	}

	/**
	 * Wait for a normal Eclipse build
	 */
	public static SLStatus waitForBuild(boolean isAuto) {
		System.out.println("Waiting for build: " + isAuto);
		try {
			Object family = isAuto ? ResourcesPlugin.FAMILY_AUTO_BUILD
					: ResourcesPlugin.FAMILY_MANUAL_BUILD;
			Job.getJobManager().join(family, null);
		} catch (OperationCanceledException e1) {
			return SLStatus.CANCEL_STATUS;
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return SLStatus.OK_STATUS;
	}

	class ConfigureJob extends AbstractSLJob {
		final Projects projects;
		final Map<String, Object> args;
		final boolean ignoreNature;

		ConfigureJob(String name, File location, boolean isAuto,
				Map<String, Object> args, boolean ignoreNature) {
			super(name);
			this.args = new HashMap<String, Object>(args);
			projects = new Projects(location, isAuto, this.args);
			args.clear();
			this.ignoreNature = ignoreNature;
		}

		public SLStatus run(SLProgressMonitor monitor) {
			if (XUtil.testing) {
				System.out.println("Do I need to do something here to wait?");
			} else {
				SLStatus s = waitForBuild(projects.isAutoBuild());
				if (s == SLStatus.CANCEL_STATUS) {
					return s;
				}
				// Clear for next build?
			}
			makeTransition(BuildState.WAITING, BuildState.BUILDING, null);			
			if (!ignoreNature) {
				System.err.println("NOT deactivating projects");
				// Clear projects that are inactive
				/*
				for (IJavaProject jp : JDTUtility.getJavaProjects()) {					
					ProjectInfo info = JavacDriver.this.projects.get(jp
							.getProject());
					if (info != null) {						
						info.setActive(Nature.hasNature(jp.getProject()));

						// Check if it was previously active, but is now a
						// dependency?
					}
				}
				*/
			}
			doBuild(projects, args, monitor, ignoreNature);
			return SLStatus.OK_STATUS;
		}
	}

	abstract class JavacJob extends AbstractSLJob {
		final Projects projects;
		/**
		 * Where the source files will be copied to
		 */
		final File targetDir;

		/**
		 * Where the source zips will be created
		 */
		final File zipDir;

		JavacJob(String name, Projects projects, File target, File zips) {
			super(name);
			this.projects = projects;
			targetDir = target;
			zipDir = zips;
		}
	}

	class CopyJob extends JavacJob {
		private final SLJob afterJob;

		CopyJob(Projects projects, File target, File zips, SLJob after) {
			super("Copying project info for " + projects.getLabel(), projects,
					target, zips);
			afterJob = after;
		}

		public SLStatus run(SLProgressMonitor monitor) {
			final long start = System.currentTimeMillis();
			try {
				for (Config config : projects.getConfigs()) {
					config.zipSources(zipDir);
				}
			} catch (IOException e) {
				return SLStatus.createErrorStatus(
						"Problem while zipping sources", e);
			}
			final long zip = System.currentTimeMillis();
			try {
				for (Config config : projects.getConfigs()) {
					config.relocateJars(targetDir);
				}
			} catch (IOException e) {
				return SLStatus.createErrorStatus("Problem while copying jars",
						e);
			}
			final long end = System.currentTimeMillis();
			System.out.println("Zipping         = " + (zip - start) + " ms");
			System.out.println("Relocating jars = " + (end - zip) + " ms");

			if (afterJob != null) {
				if (XUtil.testing) {
					afterJob.run(monitor);
				} else {
					EclipseJob.getInstance().scheduleDb(afterJob, false, false,
							Util.class.getName());
				}
			}
			return SLStatus.OK_STATUS;
		}
	}

	class AnalysisJob extends JavacJob {
		private final Projects oldProjects;
		private final boolean useSeparateJVM;

		AnalysisJob(Projects oldProjects, Projects projects, File target,
				File zips, boolean useSeparateJVM) {
			super("Running JSure on " + projects.getLabel(), projects, target,
					zips);
			if (useSeparateJVM) {
				this.oldProjects = null;
			} else {
				this.oldProjects = oldProjects;
			}
			this.useSeparateJVM = useSeparateJVM;
		}

		public SLStatus run(SLProgressMonitor monitor) {
			lastMonitor = monitor;
			projects.setMonitor(monitor);

			if (XUtil.testingWorkspace) {
				System.out.println("Clearing state before running analysis");
				ClearProjectListener.clearJSureState();
			}
			System.out.println("Starting analysis for " + projects.getLabel());
			final long start = System.currentTimeMillis();
			try {
				for (Config config : projects.getConfigs()) {
					config.copySources(zipDir, targetDir);
				}
			} catch (IOException e) {
				return SLStatus.createErrorStatus(
						"Problem while copying sources", e);
			}
			final long end = System.currentTimeMillis();
			System.out.println("Copying sources = " + (end - start) + " ms");

			JavacEclipse.initialize();
			NotificationHub.notifyAnalysisStarting();
			try {
				boolean ok = false;
				if (useSeparateJVM) {
					// Normally done by Javac, but needs to be repeated locally
					final boolean noConflict;
					if (oldProjects != null) {
						noConflict = !projects.conflictsWith(oldProjects);
						if (noConflict) {
							projects.init(oldProjects);
						} else {
							System.out
									.println("Detected a conflict between projects");
						}
					} else {
						noConflict = true;
					}
					JavacEclipse.getDefault().savePreferences(
							projects.getRunDir());
					LocalJSureJob job = makeLocalJSureJob(projects);
					SLStatus status = job.run(monitor);
					if (status == SLStatus.OK_STATUS) {
						ok = true;

						// Normally done by Javac, but needs to be repeated
						// locally
						if (oldProjects != null && noConflict) {
							final Projects merged = projects.merge(oldProjects);
							ProjectsDrop.ensureDrop(merged);
							// System.out.println("Merged projects: "+merged.getLabel());
						} else {
							ProjectsDrop.ensureDrop(projects);
						}
					} else if (status != SLStatus.CANCEL_STATUS) {
						throw status.getException();
					}
				} else {
					if (clearBeforeAnalysis || oldProjects == null) {
						ClearProjectListener.clearJSureState();

						ok = Util.openFiles(projects, true);
					} else {
						ok = Util.openFiles(oldProjects, projects, true);
					}
				}
				/*
				 * final File rootLoc =
				 * EclipseUtility.getProject(config.getProject
				 * ()).getLocation().toFile(); final File xmlLocation = new
				 * File(rootLoc, "oracle20100415.sea.xml"); final
				 * SeaSummary.Diff diff = SeaSummary.diff(config.getProject(),
				 * Sea.getDefault(), xmlLocation);
				 */
				if (!ok) {
					NotificationHub.notifyAnalysisPostponed(); // TODO fix
					if (lastMonitor == monitor) {
						lastMonitor = null;
					}
					return SLStatus.CANCEL_STATUS;
				}
			} catch (Throwable e) {
				e.printStackTrace();
				NotificationHub.notifyAnalysisPostponed(); // TODO
				if (monitor.isCanceled()) {
					if (lastMonitor == monitor) {
						lastMonitor = null;
					}
					return SLStatus.CANCEL_STATUS;
				}
				return SLStatus.createErrorStatus(
						"Problem while running JSure", e);
			} finally {
				endAnalysis();
			}
			NotificationHub.notifyAnalysisCompleted();
			// recordViewUpdate();

			if (lastMonitor == monitor) {
				lastMonitor = null;
			}

			return SLStatus.OK_STATUS;
		}

		private LocalJSureJob makeLocalJSureJob(final Projects projects) {
			System.out.println("run = " + projects.getRun());
			final String msg = "Running JSure for " + projects.getLabel();
			final int port = LocalJSureJob.DEFAULT_PORT;
			ILocalJSureConfig cfg = new ILocalJSureConfig() {
				public boolean isVerbose() {
					return true;
				}

				public String getTestCode() {
					return TestCode.NONE.name();
				}

				public int getMemorySize() {
					return 1024;
				}

				public String getPluginDir(String id, boolean required) {
					try {
						return EclipseUtility.getDirectoryOf(id);
					} catch (IllegalStateException e) {
						if (required) {
							throw e;
						} else {
							return null;
						}
					}
				}

				public String getRunDirectory() {
					return projects.getRunDir().getAbsolutePath();
				}
			};
			return new LocalJSureJob(msg, projects.size(), cfg, port);
		}

		protected void endAnalysis() {
			final RebuildState state = makeTransition(BuildState.BUILDING,
					null, null);
			if (state != null) {
				EclipseJob.getInstance().scheduleDb(
						new AbstractSLJob("Rebuilding JSure") {
							public SLStatus run(SLProgressMonitor monitor) {
								/*
								 * try { Thread.sleep(3000); } catch
								 * (InterruptedException e) {
								 * e.printStackTrace(); }
								 */
								System.out.println("Rebuilding ...");
								configureBuild(
										EclipseUtility.getWorkspacePath(),
										state == RebuildState.AUTO, false);
								return SLStatus.OK_STATUS;
							}
						});
			}
		}
	}

	public void setArg(String key, Object value) {
		args.put(key, value);
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
		printToScript(ScriptCommands.EXPECT_BUILD_FIRST + ' ' + computePrefix()
				+ '/' + name);

		recordFilesToAnalyze(p);
	}

	private void recordFilesToAnalyze(Projects p) throws FileNotFoundException {
		final String name = "expectedAnalysis" + getId() + ".txt";
		final File file = new File(scriptResourcesDir, name);
		p.setArg(Util.RECORD_ANALYSIS, file);
		printToScript(ScriptCommands.EXPECT_ANALYSIS_FIRST + ' '
				+ computePrefix() + '/' + name);
	}

	private void checkForExpectedSourceFiles(Projects p, File expected)
			throws IOException {
		System.out.println("Checking source files expected for build");
		final Set<String> cus = RegressionUtility.readLinesAsSet(expected);
		boolean somethingToBuild = false;
		for (Config c : p.getConfigs()) {
			for (JavaSourceFile f : c.getFiles()) {
				if (!cus.remove(f.relativePath)) {
					throw new IllegalStateException("Building extra file: "
							+ f.relativePath);
				} else {
					System.out.println("Correctly building: " + f.relativePath);
				}
				somethingToBuild = true;
			}
		}
		if (!cus.isEmpty()) {
			if (somethingToBuild) {
				throw new IllegalStateException("File not built: "
						+ cus.iterator().next());
			} else {
				throw new IllegalStateException("No files built: "
						+ cus.iterator().next());
			}
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
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
			System.out.println("Ignoring change2 to "
					+ event.getResource().getName());
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
			System.out.println("Handling addition as a new project "
					+ delta.getResource());
			if (script != null) {
				printToScript(ScriptCommands.CREATE_PROJECT + ' '
						+ delta.getResource().getName());
			}
			return;
		case IResourceDelta.REMOVED:
			System.out.println("Handling removal of project "
					+ delta.getResource());
			if (script != null) {
				printToScript(ScriptCommands.DELETE_PROJECT + ' '
						+ delta.getResource().getName());
			}
			return;
		case IResourceDelta.CHANGED:
			if (delta.getFlags() != IResourceDelta.OPEN) {
				System.out.println("Ignoring change5 to project "
						+ delta.getResource() + ": " + delta.getFlags());
				/*
				 * for(IResourceDelta d : delta.getAffectedChildren()) {
				 * changed(d); }
				 */
				return;
			}
			final IProject p = (IProject) delta.getResource();
			if (p.isOpen()) {
				System.out.println("Handling opening project "
						+ delta.getResource());
				if (script != null) {
					printToScript(ScriptCommands.OPEN_PROJECT + ' '
							+ delta.getResource().getName());
				}
			} else {
				System.out.println("Handling closing project "
						+ delta.getResource());
				if (script != null) {
					printToScript(ScriptCommands.CLOSE_PROJECT + ' '
							+ delta.getResource().getName());
				}
			}
			return;
		default:
			System.out.println("Ignoring change3 to " + delta.getResource());
			return;
		}
	}
}
