package com.surelogic.jsure.client.eclipse.analysis;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.*;
import com.surelogic.common.FileUtility.*;
import com.surelogic.common.eclipse.*;
import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.*;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;

import difflib.*;

import edu.cmu.cs.fluid.dc.*;
import edu.cmu.cs.fluid.dc.Plugin;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.xml.*;
import edu.cmu.cs.fluid.util.*;

public class JavacDriver implements IResourceChangeListener {
	/**
	 * Clear all the JSure state before each build
	 */
	private static final boolean clearBeforeAnalysis = false;
	
	enum BuildState {
		// Null means no build right now
		WAITING, BUILDING
	}
	enum RebuildState {
		// Null means no need to rebuild
		AUTO, FULL
	}
	
	/**
	 * If true, create common projects for shared jars
	 * Otherwise, jars in different are treated as if they're completely unique
	 * 
	 * Creating separate projects for shared jars doesn't work, 
	 * due to dependencies on other jars, esp. the JRE	 
	 */
	private static final boolean shareCommonJars = false;
	
	//private final List<IProject> building = new ArrayList<IProject>();
	private final Map<String,Object> args = new HashMap<String, Object>();  
	private final Map<IProject, ProjectInfo> projects = new HashMap<IProject, ProjectInfo>();
	private final File tempDir;
	private final File scriptResourcesDir;
	private final PrintStream script;
	private final ZipInfo info;
	private boolean ignoreNextCleanup = true;
	
	/**
	 * State that needs to be atomically modified
	 */
	private BuildState buildState = null;
	private RebuildState rebuildQueue = null;
	
	synchronized RebuildState makeTransition(BuildState before, BuildState after, RebuildState rebuild) {
		if (buildState != before) {
			throw new IllegalStateException("Build state isn't "+before+": "+buildState);
		}
		buildState = after;
		try {
			return rebuildQueue;
		} finally {
			rebuildQueue = rebuild;
		}
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
		final String temp = XUtil.recordScript();
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
			// Clean out the directory
			for(File f : scriptResourcesDir.listFiles()) {
				FileUtility.recursiveDelete(f);
			}
			PrintStream out = null;
			ZipInfo zipInfo = null;
			File tmp = null;
			final File scriptF = new File(workspace, proj+File.separatorChar+ScriptCommands.NAME);
			try {
				if (scriptF.exists()) {
					System.out.println("Deleting old script "+scriptF);
					scriptF.delete();
				}
				final File zip =  new File(workspace, proj+".zip");
				if (zip.exists()) {
					System.out.println("Deleting existing "+zip);
					zip.delete();
				}
				zipInfo = FileUtility.zipDirAndMore(new File(workspace, proj), zip);
				out = new PrintStream(scriptF);
				FileUtility.deleteTempFiles(filter);
				tmp = filter.createTempFile(); 
				tmp.delete();
				tmp.mkdir();
			} catch (IOException e) {
				e.printStackTrace();
			}
			tempDir = tmp;
			script  = (tmp == null) ? null : out;
			info    = zipInfo;
		} else {			
			script = null;
			scriptResourcesDir = null;
			tempDir = null;
			info = null;
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
				System.out.println("Already created a copy: "+r.getFullPath());
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
	 * @param cus
	 */
	private void cacheCompUnits(List<ICompilationUnit> cus) {
		// FIX how do I map back to the right resource?
		for(ICompilationUnit cu : cus) {
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
		final File cached     = computeCopyFile(tempDir, r);		
		final File changed    = r.getLocation().toFile();
		List<String> original = fileToLines(cached);
		List<String> revised  = fileToLines(changed);

		// Compute diff. Get the Patch object. Patch is the container for computed deltas.
		final Patch patch = DiffUtils.diff(original, revised);		
		for (Delta delta: patch.getDeltas()) {
			System.out.println(delta);
		}
		final List<String> diff = 
			DiffUtils.generateUnifiedDiff(r.getName(), r.getName(), original, patch, 5);		
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
		final String base = r.getFullPath()+"."+getId()+".patch";
		File f            = new File(scriptResourcesDir, base);
		String name       = base;
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
		for(String line : lines) {
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
		final int slash   = temp.indexOf('/');
		final String prefix;
		if (slash < 0) {
			prefix = '/'+temp+'/'+"script";
		} else {
			prefix = '/'+temp;
		}
		return prefix;
	}
	
	private void scriptChanges(List<Pair<IResource, Integer>> resources) {
		if (resources.size() > 1) {
			printToScript("unset "+ScriptCommands.AUTO_BUILD);
		}
		for(Pair<IResource, Integer> p : resources) {
			final IResource r = p.first();
			final String rName = r.getName();
			if (r.getType() != IResource.FILE) {
				System.out.println("Ignoring "+rName);
				continue;
			} else if (!r.getName().endsWith(".java")) {
				System.out.println("Ignoring non-Java file: "+rName);
				continue;
			}
			final String prefix = computePrefix();
			final String path   = r.getFullPath().toString();
			switch (p.second()) {
			case IResourceDelta.ADDED:
				String name = copyAsResource(scriptResourcesDir, r);
				copyAsResource(tempDir, r);
				
				// Use the directory that we'll be importing into
				final int lastSlash = path.lastIndexOf('/');
				final String dest   = lastSlash < 0 ? path : path.substring(0, lastSlash);			
				printToScript(ScriptCommands.IMPORT+' '+dest+' '+prefix+name);
				break;
			case IResourceDelta.CHANGED:
				String patch = createPatch(r);
				copyAsResource(tempDir, r, true); // Update the patched file
				printToScript(ScriptCommands.PATCH_FILE+' '+path+' '+prefix+patch);
				break;
			case IResourceDelta.REMOVED:
				printToScript(ScriptCommands.DELETE_FILE+' '+path);
				break;
			default:
				System.out.println("Couldn't handle flag: "+p.second());
			}
		}
		if (resources.size() > 1) {
			printToScript("set "+ScriptCommands.AUTO_BUILD);
		}
	}

	public void recordProjectAction(String action, IProject p) {
		if (script != null) {		
			printToScript(action+' '+p.getName());
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
			if (projs == null) {
				projs = JDTUtility.getProjects();
			} 
			*/
 			for(IProject p : projs) {
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
        	final String prefix = "expectedResults"+getId();
    		final String name   = prefix+SeaSnapshot.SUFFIX;
    		final File location = new File(scriptResourcesDir, name);        		
			try {
				final String path = computePrefix();
				Sea.getDefault().updateConsistencyProof();
				SeaSummary.summarize("workspace", Sea.getDefault(), location);					
            	printToScript(ScriptCommands.COMPARE_RESULTS+" workspace "+path+'/'+name+
            			      " "+path+"/../"+prefix+RegressionUtility.JSURE_SNAPSHOT_DIFF_SUFFIX);
			} catch (IOException e) {
				e.printStackTrace();
			}                
        }
	}
	
	public void stopScripting() {
		if (script != null) {
			script.close();
			try {
				final File baseDir = scriptResourcesDir.getParentFile();
				info.zipDir(baseDir, scriptResourcesDir);				
				info.zipFile(baseDir, new File(baseDir, ScriptCommands.NAME));

				final File settings = new File(baseDir, ScriptCommands.ANALYSIS_SETTINGS);
				if (!settings.exists()) {
					Plugin.getDefault().writePrefsToXML(settings);
					info.zipFile(baseDir, settings);
				}
				final File props = new File(baseDir, ScriptCommands.TEST_PROPERTIES);
				if (!props.exists()) {
					PrintWriter pw = new PrintWriter(props);
					pw.print("moreVMargs=-Dfluid.ir.versioning=Versioning.Off ");
					pw.print("-Ddoublechecker.useSuperRoots=SuperRoots.Off -Ddc.show.private=true ");
					pw.print("-Dsurelogic.useNewParser=Parser.On -Dxml.useNewParser=Parser.On ");
					pw.println("-Drules.useNewScopedPromises=Promise.On");
					pw.close();
					info.zipFile(baseDir, props);
				}
				info.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static final TempFileFilter filter = new TempFileFilter("scriptTemp", ".dir");
	
	private static final JavacDriver prototype = new JavacDriver();
	static {		
		if (prototype.script != null) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(prototype, IResourceChangeEvent.PRE_BUILD);
			//IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE
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
			for(Pair<IResource, Integer> p : resources) {
				if (p.second() == IResourceDelta.REMOVED && p.first().getName().endsWith(".java")) {
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
				Iterable<ICompilationUnit> result = new ArrayList<ICompilationUnit>(cuDelta);
				update(allCompUnits, cuDelta, removed);
				return result;
			}
			return allCompUnits;	 
		}
		
		/**
		 * Adds itself to projects to make sure that it's not created multiple times
		 */
		Config makeConfig(final Projects projects, boolean all) throws JavaModelException {
			final IJavaProject jp = JDTUtility.getJavaProject(project.getName());		
			scanForJDK(projects, jp);
			
			Config config = new ZippedConfig(project.getName(), false);
			projects.add(config);
			setOptions(config);
			
			for(IResource res : getRemovedResources()) {
				final File f = res.getLocation().toFile();
				config.addRemovedFile(f);
			}
			for(JavaSourceFile jsf : convertCompUnits(config, all ? getAllCompUnits() : getDelta())) {
				config.addFile(jsf);
			}
			addDependencies(projects, config, project, false);
			return config;
		}
		
		private void setOptions(Config config) {
			IJavaProject jp = JDTUtility.getJavaProject(config.getProject());
			int version = JDTUtility.getMajorJavaVersion(jp);
			config.setOption(Config.SOURCE_LEVEL, version);
			//System.out.println(config.getProject()+": set to level "+version);
		}		
		
		void addDependencies(Projects projects, Config config, IProject p, boolean addSource) throws JavaModelException {
			final IJavaProject jp = JDTUtility.getJavaProject(p.getName());			
			// TODO what export rules?
			scanForJDK(projects, jp);
			
			for(IClasspathEntry cpe : jp.getResolvedClasspath(true)) {				
				// TODO ignorable since they'll be handled by the compiler
				//cpe.getAccessRules();
				//cpe.combineAccessRules();
				
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					if (addSource) {
						addSourceFiles(config, cpe);
					}
					config.addToClassPath(config);
					break;
				case IClasspathEntry.CPE_LIBRARY:
					//System.out.println("Adding "+cpe.getPath()+" for "+p.getName());
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
					final IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
					final ProjectInfo info = JavacDriver.this.projects.get(proj);
					final Config dep;
					if (info != null) {
						final boolean hasDeltas = info.hasDeltas();
						dep = info.makeConfig(projects, hasDeltas);
					} else {
						dep = new ZippedConfig(projName, cpe.isExported());								
						projects.add(dep);
						setOptions(dep);
					}
					config.addToClassPath(dep);

					if (info == null) {
						addDependencies(projects, dep, proj, true);													
					}
					break;
				default:
					System.out.println("Unexpected: "+cpe);
				}
			}
		}

		private void addSourceFiles(Config config, IClasspathEntry cpe) {
			// TODO handle multiple deltas?
			/*
			final File dir = EclipseUtility.resolveIPath(cpe.getPath());
			final File[] excludes = new File[cpe.getExclusionPatterns().length];
			int i=0;
			for(IPath xp : cpe.getExclusionPatterns()) {
				excludes[i] = EclipseUtility.resolveIPath(xp);
				i++;
			}
			*/						
			IContainer root = (IContainer) 
			    ResourcesPlugin.getWorkspace().getRoot().findMember(cpe.getPath());
			final IResource[] excludes = new IResource[cpe.getExclusionPatterns().length];
			int i=0;
			for(IPath xp : cpe.getExclusionPatterns()) {
				excludes[i] = root.findMember(xp);
				i++;
			}
			addJavaFiles(root, config, excludes);			
		}
		
		private void addJavaFiles(IContainer dir, Config config, IResource... excluded) {
			try {
				addJavaFiles("", dir, config, excluded);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		private void addJavaFiles(String pkg, IContainer dir, Config config, IResource[] excluded) throws CoreException {
			for(IResource x : excluded) {
				if (dir.equals(x)) {
					return;
				}
			}
			if (dir == null || !dir.exists()) {
				return;
			}
			//System.out.println("Scanning "+dir.getAbsolutePath());
			boolean added = false;
			for(IResource r : dir.members()) {
				if (r instanceof IFile && r.getName().endsWith(".java")) {
					final ICompilationUnit icu = JavaCore.createCompilationUnitFrom((IFile) r);
					if ((icu != null) && (icu.getJavaProject().isOnClasspath(icu))) {
						final File f = r.getLocation().toFile();
						//System.out.println("Found source file: "+f.getPath());
						String typeName = f.getName().substring(0, f.getName().length()-5);
						String qname    = pkg.length() == 0 ? typeName : pkg+'.'+typeName;
						config.addFile(new JavaSourceFile(qname, f, f.getAbsolutePath()));
						if (!added) {
							added = true;
							/*
						if (debug) {
							System.out.println("Found java files in "+pkg);
						}
							 */
							config.addPackage(pkg);
						}
					}
				}
				if (r instanceof IContainer) {				
					final String newPkg = pkg == "" ? r.getName() : pkg+'.'+r.getName();
					addJavaFiles(newPkg, (IContainer) r, config, excluded);
				}
	    	}
		}
		
		/**
		 * Create a project/config for a shared jar
		 */
		private JavacProject makeJarConfig(Projects projects, File f, String name) {
			System.out.println("Creating shared jar: "+name);
			final Config config = new Config(name, true);
			config.addJar(f, true);
			return projects.add(config);
		}

		private void scanForJDK(Projects projects, IJavaProject jp) throws JavaModelException {
			for(IClasspathEntry cpe : jp.getRawClasspath()) {								
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
					final IClasspathContainer cc = JavaCore.getClasspathContainer(cpe.getPath(), jp);					
					if (cc.getDescription().startsWith(JavacTypeEnvironment.JRE_LIBRARY)) { // HACK
						JavacProject jcp = findJRE(projects, cc);
						if (jcp == null) {
							projects.add(makeConfig(projects, cc));							
						}
						return;
					}			
				}
			}
		}
		
		private JavacProject findJRE(Projects projects, final IClasspathContainer cc) {
			final String name = cc.getPath().toPortableString();
			JavacProject jcp = projects.get(name);
			if (jcp == null) {
				// Not found by name, so check for existing JREs
				for(JavacProject p : projects) {
					if (p.getName().startsWith(JavacTypeEnvironment.JRE_NAME) &&
						compareJREs(p.getConfig(), cc)) {
						return p;
					}
				}
			}
			return jcp;
		}
		
		private boolean compareJREs(Config c, final IClasspathContainer cc) {
			final IClasspathEntry[] cpes = cc.getClasspathEntries();
			int i = 0;
			for(IClassPathEntry e : c.getClassPath()) {
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
		private Config makeConfig(Projects projects, final IClasspathContainer cc) {
			final String name = cc.getPath().toPortableString();
			final Config config = new Config(name, true);
			for(IClasspathEntry cpe : cc.getClasspathEntries()) {
				switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY:
					final File f = EclipseUtility.resolveIPath(cpe.getPath());
					//System.out.println("Adding "+f+" for "+cc.getDescription());
					config.addJar(f, true);
					projects.mapToProject(f, name);
					break;
				default:
					throw new IllegalStateException("Got entryKind: "+cpe.getEntryKind());
				}
			}
			JavacEclipse.getDefault().setPreference(IDE.DEFAULT_JRE, name);
			return config;
		}
		
		/**
		 * Either add/remove as needed
		 * @param removed2 
		 */
		void update(Collection<ICompilationUnit> all, Collection<ICompilationUnit> cus, 
				    Set<IResource> removed) {
			// Filter out removed files
			final Iterator<ICompilationUnit> it = all.iterator();
			while (it.hasNext()) {
				final ICompilationUnit cu = it.next();
				if (removed.contains(cu.getResource())) {
					it.remove();
				}
			}
			// Add in changed ones
			for(ICompilationUnit cu : cus) {
				// TODO use a Set instead?
				if (cu.getResource().exists()) {
					if (!all.contains(cu)) {
						all.add(cu);
						//System.out.println("Added:   "+cu.getHandleIdentifier());
					} else {
						//System.out.println("Exists:  "+cu.getHandleIdentifier());
					}
				} else {
					all.remove(cu);
					//System.out.println("Deleted: "+cu.getHandleIdentifier());
				}
			}
			updated = true;
		}
	}
	
	void preBuild(final IProject p) {		
		System.out.println("Pre-build for "+p);
		/*
		if (building.isEmpty()) {
			// First project build, so populate with active projects
			for(IJavaProject jp : JDTUtility.getJavaProjects()) {
				final IProject proj = jp.getProject();
				if (Nature.hasNature(proj)) {
					building.add(proj);
				}
			}
		}
		*/
	}
	
	/**
	 * Register resources
	 */
	@SuppressWarnings("unchecked")
	void registerBuild(IProject project, Map args,
			           List<Pair<IResource, Integer>> resources, 
			           List<ICompilationUnit> cus) {
		final int k = getBuildKind(args);
		if (k == IncrementalProjectBuilder.CLEAN_BUILD || 
			k == IncrementalProjectBuilder.FULL_BUILD) {
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
		final String kind = (String) args.get(Majordomo.BUILD_KIND);		
		return Integer.parseInt(kind);
	}
	@SuppressWarnings("unchecked")
	void configureBuild(Map args) {
		final int k = getBuildKind(args);
		configureBuild((k & IncrementalProjectBuilder.AUTO_BUILD) == IncrementalProjectBuilder.AUTO_BUILD);
	}
	
	public void configureBuild(boolean isAuto /*IProject p*/) {	    
		//System.out.println("Finished 'build' for "+p);
		/*
		//ProjectDrop.ensureDrop(p.getName(), p);
		final ProjectInfo info = projects.get(p);
		if (info == null) {
			return; // No info!
		}
		*/
		/*
		// Check if any projects are still building
		building.remove(p);
		if (!building.isEmpty()) {
			System.out.println("Still waiting for "+building);
			return;
		}
		*/	
		
		// TODO this needs to be run after ALL the info is collected
        JavacEclipse.initialize();
        if (!XUtil.testing) {
        	System.out.println("Configuring analyses for build");
        	final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        	((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs(store);
        }
        ConfigureJob configure = new ConfigureJob("Configuring JSure build", isAuto, args);
        synchronized (this) {
        	if (buildState == null) {
        		buildState = BuildState.WAITING;
        	} else {        		
        		// Build already going        		
        		System.out.println("Already started ConfigureJob: "+buildState);	    	
        		if (buildState == BuildState.BUILDING) {
        			// We need to do another build, since there might be some updates 
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
        			// Ok to ignore, because this will be handled by the currently waiting build
        		}
        		return;
        	}	   
        	// Only if there's no build already
	    	System.out.println("Starting to configure JSure build");
		    ProjectsDrop pd = ProjectsDrop.getDrop();
		    if (pd != null) {
		    	for(JavacProject jp : ((Projects) pd.getIIRProjects())) {
		    		System.out.println("Deactivating "+jp);
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
	
	private void doBuild(final Projects newProjects, Map<String, Object> args, SLProgressMonitor monitor) {
		try {
			if (!XUtil.testing) {
				  System.out.println("Configuring analyses for doBuild");
				final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs(store);
			}
			//final boolean hasDeltas = info.hasDeltas();
		    makeProjects(newProjects);	    

		    final File dataDir = 
		        //new File(IDE.getInstance().getStringPreference(IDEPreferences.DATA_DIRECTORY));
		        PreferenceConstants.getJSureDataDirectory();
		    final String time = SLUtility.toStringHMS(new Date());
		    final String name = newProjects.getShortLabel()+' '+time.replace(':', '-');		   		    
		    final File zips   = new File(dataDir, name+"/zips");
		    final File target = new File(dataDir, name+"/srcs");
		    target.mkdirs();
		    newProjects.setRun(name);

		    JSureHistoricalSourceView.setLastRun(newProjects, new ISourceZipFileHandles() {
                public Iterable<File> getSourceZips() {
                    return Arrays.asList(zips.listFiles());
                }		        
		    });

		    Projects oldProjects = (Projects) ProjectsDrop.getProjects();		    
		    if (!clearBeforeAnalysis && oldProjects != null) {
		    	findModifiedFiles(newProjects, oldProjects);
		    }		    
		    
		    AnalysisJob analysis = new AnalysisJob(oldProjects, newProjects, target, zips);
		    CopyJob copy = new CopyJob(newProjects, target, zips, analysis);
			if (script != null) {
				recordFilesToAnalyze(newProjects);
			}
		    if (XUtil.testing) {
		    	final File expected = (File) args.get(ScriptCommands.EXPECT_BUILD);
		    	if (expected != null && expected.exists()) {
		    		checkForExpectedSourceFiles(newProjects, expected);
		    	}
		    	copy.run(new NullSLProgressMonitor());
		    } else {
		    	EclipseJob.getInstance().scheduleWorkspace(copy);
		    }
		} catch(Exception e) {
		    System.err.println("Unable to make config for JSure");
		    e.printStackTrace();
		    if (XUtil.testing) {
		    	throw (RuntimeException) e;
		    }
		    return;
		}
	}

	private void findModifiedFiles(final Projects newProjects, Projects oldProjects) {
		//System.out.println("Checking for files modified after "+oldProjects.getDate());
		final Map<IJavaProject, Date> times = new HashMap<IJavaProject, Date>();
		for(JavacProject jp : newProjects) {
			// Check if we used it last time			
			if (oldProjects.get(jp.getName()) != null) {
				//System.out.println("Checking for "+jp.getName());
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
		for(ICompilationUnit icu : JDTUtility.modifiedCompUnits(times, new NullProgressMonitor())) {			
			byProj.put(icu.getJavaProject().getElementName(), icu);
		}
		for(IJavaProject ijp : times.keySet()) {
			final JavacProject jp = newProjects.get(ijp.getElementName());
			if (jp != null) {
				final Collection<ICompilationUnit> cus = byProj.get(ijp.getElementName());
				final Config c = jp.getConfig();
				if (cus != null && cus.size() > 0) {
					System.out.println(ijp.getElementName()+" has "+cus.size()+" modified files");		
					try {
						c.intersectFiles(convertCompUnits(c, cus));
					} catch (JavaModelException e1) {
						// Suppressed, since it's an optimization
					}
				} else {
					// No changed files, so clear it out
					c.intersectFiles(Collections.<JavaSourceFile>emptyList());
				}
			}
		}
	}
	
	private void findSharedJars(final Projects projects) {
		if (!shareCommonJars) {
			return;
		}
		/*
		try {
			final Map<File,File> shared = new HashMap<File, File>();
			for(IJavaProject p : JDTUtility.getJavaProjects()) {
				for(IClasspathEntry cpe : p.getResolvedClasspath(true)) {				
					switch (cpe.getEntryKind()) {
					case IClasspathEntry.CPE_LIBRARY:
						final IPath path = cpe.getPath();
						final File f = EclipseUtility.resolveIPath(path);
						if (shared.containsKey(f)) {							
							//System.out.println("Repeated view: "+f);
							shared.put(f, f);
						} else if (f != null) {
							//System.out.println("First view:    "+f);
							shared.put(f, null); // Seen once
						}						
					}
				}
			}
			// Create mappings for shared jars
			for(File path : shared.keySet()) {
				File f = shared.get(path);
				if (f != null) {
					projects.mapToProject(path, f.getAbsolutePath());
				} else {
					// Ignore jars only seen once
				}
			}			
		} catch (JavaModelException e) {
			return;
		}	
		*/	
	}
	
	// TODO how to set up for deltas?
	private Projects makeProjects(final Projects projects) throws JavaModelException {
		findSharedJars(projects);
		
		List<ProjectInfo> infos = new ArrayList<ProjectInfo>(this.projects.values());
		for(ProjectInfo info : infos) {
			if (!projects.contains(info.project.getName())) {
				if (info.isActive()) {
					info.makeConfig(projects, !info.hasDeltas());	
				} else {
					// Otherwise, it's inactive
					continue;
				}
			} else {
				// Already added as a dependency?
				info.setActive(true);
			}
			Config config = projects.get(info.project.getName()).getConfig();
			config.setOption(Config.AS_SOURCE, true);
		}
		
		// Remove inactive projects? 
		for(ProjectInfo info : infos) {
			if (!info.isActive()) {
				this.projects.remove(info.project);
			}
		}
		return projects;

	}
	
	private Collection<JavaSourceFile> convertCompUnits(Config config, final Iterable<ICompilationUnit> cus) 
	throws JavaModelException {
		List<JavaSourceFile> files = new ArrayList<JavaSourceFile>();
		for(ICompilationUnit icu : cus) {				
			final IPath path = icu.getResource().getFullPath();
			final IPath loc = icu.getResource().getLocation();
			final File f = loc.toFile();
			String qname;
			if (f.exists()) {
				String pkg = null;
				for(IPackageDeclaration pd : icu.getPackageDeclarations()) {
					config.addPackage(pd.getElementName());
					pkg = pd.getElementName();
				}
				qname = icu.getElementName();
				if (qname.endsWith(".java")) {
					qname = qname.substring(0, qname.length()-5);
				}
				if (pkg != null) {
					qname = pkg+'.'+qname;
				}
			} else { // Removed
				qname = f.getName();
			}
			files.add(new JavaSourceFile(qname, f, path.toPortableString()));
		}
		return files;
	}
	
	static class ZippedConfig extends Config {
		ZippedConfig(String name, boolean isExported) {
			super(name, isExported);
		}
		@Override
		protected Config newConfig(String name, boolean isExported) {
			return new ZippedConfig(name, isExported);
		}		
		@Override
		public void zipSources(File zipDir) throws IOException {
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getProject());
			final SourceZip srcZip = new SourceZip(project);
			File zipFile           = new File(zipDir, project.getName()+".zip");
			if (!zipFile.exists()) {
				zipFile.getParentFile().mkdirs();
				srcZip.generateSourceZip(zipFile.getAbsolutePath(), project);
			} else {
				//System.out.println("Already exists: "+zipFile);
			}			
			super.zipSources(zipDir);
		}
		@Override
		public void copySources(File zipDir, File targetDir) throws IOException {
            final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(getProject());
            targetDir.mkdir();
            
            File projectDir = new File(targetDir, project.getName());
            File zipFile    = new File(zipDir, project.getName()+".zip");
            ZipFile zf      = new ZipFile(zipFile);
            
            // Get class mapping (qname->zip path)
            Properties props = new Properties();
            ZipEntry mapping = zf.getEntry(AbstractJavaZip.CLASS_MAPPING);            
            props.load(zf.getInputStream(mapping));

            // Reverse mapping
            final Map<String,List<String>> path2qnames = new HashMap<String,List<String>>();
            //int count = 0;
            for(Map.Entry<Object,Object> e : props.entrySet()) {
                String path = (String) e.getValue();
                List<String> l = path2qnames.get(path);
                if (l == null) {
                    l = new ArrayList<String>();
                    path2qnames.put(path, l);
                }                
                l.add((String) e.getKey());
                //count++;
            }
            //System.out.println(getProject()+": class mapping "+count);
            /*
            for(JavaSourceFile f : getFiles()) {
            	System.out.println(getProject()+": "+f.relativePath);
            }
            */
            final List<JavaSourceFile> srcFiles = new ArrayList<JavaSourceFile>();
            FileUtility.unzipFile(zf, projectDir, new UnzipCallback() {				
				public void unzipped(ZipEntry ze, File f) {
	                // Finish setting up srcFiles
	                if (ze.getName().endsWith(".java")) {
	                    final List<String> names = path2qnames.get(ze.getName());
	                    if (names != null) {
	                        for(String name : names) {
	                            //System.out.println("Mapping "+name+" to "+f.getAbsolutePath());
	                            srcFiles.add(new JavaSourceFile(name.replace('$', '.'), f, null));
	                        }
	                    } else if (ze.getName().endsWith("/package-info.java")) {
	                        System.out.println("What to do about package-info.java?");
	                    } else {
	                        System.err.println("Unable to get qname for "+ze.getName());
	                    }
	                } else {
	                	//System.out.println("Not a java file: "+ze.getName());
	                }
				}
			});
            
            this.setFiles(srcFiles);
            super.copySources(zipDir, targetDir);
        }  
	}
	
	class ConfigureJob extends AbstractSLJob {
		final Projects projects;
		final Map<String, Object> args;
		
		ConfigureJob(String name, boolean isAuto, Map<String, Object> args) {
			super(name);
			projects = new Projects(isAuto);
			this.args = new HashMap<String, Object>(args);
			args.clear();
		}

		public SLStatus run(SLProgressMonitor monitor) {
			if (XUtil.testing) {
				System.out.println("Do I need to do something here to wait?");
			} else {
				try {
					Object family = projects.isAutoBuild() ?
							ResourcesPlugin.FAMILY_AUTO_BUILD : ResourcesPlugin.FAMILY_AUTO_BUILD;
					Job.getJobManager().join(family, null);
				} catch (OperationCanceledException e1) {
					return SLStatus.CANCEL_STATUS;
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// Clear for next build?
			}
			makeTransition(BuildState.WAITING, BuildState.BUILDING, null);
			
			// Clear projects that are inactive
			for(IJavaProject jp : JDTUtility.getJavaProjects()) {
				ProjectInfo info = JavacDriver.this.projects.get(jp.getProject());
				if (info != null) {
					info.setActive(Nature.hasNature(jp.getProject()));
	
					// Check if it was previously active, but is now a
					// dependency?
				}
			}			
			doBuild(projects, args, monitor);
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
            super("Copying project info for "+projects.getLabel(), projects, target, zips);
            afterJob = after;
        }

        public SLStatus run(SLProgressMonitor monitor) {        	
        	final long start = System.currentTimeMillis();
        	try {
        		for(Config config : projects.getConfigs()) {
        			config.zipSources(zipDir);           
        		}
            } catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while zipping sources", e);
            }
            try {
            	for(Config config : projects.getConfigs()) {
            		config.relocateJars(targetDir);
            	}
			} catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while copying jars", e);
			}
            final long end = System.currentTimeMillis();
            System.out.println("Copying = "+(end-start)+" ms");
            
            if (afterJob != null) {
        	    if (XUtil.testing) {
        	    	afterJob.run(monitor);
        	    } else {
        	    	EclipseJob.getInstance().scheduleDb(afterJob, false, false, Util.class.getName());
        	    }
            }                   
            return SLStatus.OK_STATUS;
        }   
	}
	
	class AnalysisJob extends JavacJob {
		private final Projects oldProjects;
		
        AnalysisJob(Projects oldProjects, Projects projects, File target, File zips) {
            super("Running JSure on "+projects.getLabel(), projects, target, zips);
            this.oldProjects = oldProjects;
        }

        public SLStatus run(SLProgressMonitor monitor) {
        	lastMonitor = monitor;
        	projects.setMonitor(monitor);
        	
        	if (XUtil.testingWorkspace) {
        		System.out.println("Clearing state before running analysis");
        		ClearProjectListener.clearJSureState();
        	}        	
        	System.out.println("Starting analysis for "+projects.getLabel());
        	try {
        		for(Config config : projects.getConfigs()) {
        			config.copySources(zipDir, targetDir);
        		}
            } catch (IOException e) {
                return SLStatus.createErrorStatus("Problem while copying sources", e);
            }
            
            JavacEclipse.initialize();
            NotificationHub.notifyAnalysisStarting();
            try {
            	boolean ok;            	
            	if (clearBeforeAnalysis || oldProjects == null) {
            		ClearProjectListener.clearJSureState();
            		ok = Util.openFiles(projects);
            	} else {
            		ok = Util.openFiles(oldProjects, projects);
            	}
            	/*
            	final File rootLoc = EclipseUtility.getProject(config.getProject()).getLocation().toFile();
            	final File xmlLocation = new File(rootLoc, "oracle20100415.sea.xml");
            	final SeaSummary.Diff diff = SeaSummary.diff(config.getProject(), Sea.getDefault(), 
            			xmlLocation);
            			*/
            	if (!ok) {
            	    NotificationHub.notifyAnalysisPostponed(); // TODO fix
            	    if (lastMonitor == monitor) {
            	    	lastMonitor = null;
            	    }
                  	return SLStatus.CANCEL_STATUS;
            	}
            } catch (Exception e) {
                NotificationHub.notifyAnalysisPostponed(); // TODO
                if (monitor.isCanceled()) {
                    if (lastMonitor == monitor) {
                    	lastMonitor = null;
                    }
                	return SLStatus.CANCEL_STATUS;
                }
                return SLStatus.createErrorStatus("Problem while running JSure", e);
            } finally {
            	endAnalysis();      	
            }
            NotificationHub.notifyAnalysisCompleted();
            //recordViewUpdate();
            
            if (lastMonitor == monitor) {
            	lastMonitor = null;
            }
            
            return SLStatus.OK_STATUS;
        }  
        
	    
	    protected void endAnalysis() {
	    	final RebuildState state = makeTransition(BuildState.BUILDING, null, null);   	    
    		if (state != null) {
    			EclipseJob.getInstance().scheduleDb(new AbstractSLJob("Rebuilding JSure") {					
					public SLStatus run(SLProgressMonitor monitor) {
					    /*
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
*/
			  			System.out.println("Rebuilding ...");
		    			configureBuild(state == RebuildState.AUTO);
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
	
	private void recordFilesToAnalyze(Projects p) throws FileNotFoundException {
		final String name = "expectedBuild"+getId()+".txt";
		final File file   = new File(scriptResourcesDir, name);
		
		final PrintWriter pw = new PrintWriter(file);
		for(Config c : p.getConfigs()) {
			for(JavaSourceFile f : c.getFiles()) {
				pw.println(f.relativePath);
			}
		}
		pw.close();
		printToScript(ScriptCommands.EXPECT_BUILD_FIRST+' '+computePrefix()+'/'+name);
	}
	
	private void checkForExpectedSourceFiles(Projects p, File expected) throws IOException {
		System.out.println("Checking expected source files");
		final Set<String> cus = readExpected(expected);
		for(Config c : p.getConfigs()) {
			for(JavaSourceFile f : c.getFiles()) {
				if (!cus.remove(f.relativePath)) {
					throw new IllegalStateException("Building extra file: "+f.relativePath);
				}
			}
		}
		if (!cus.isEmpty()) {
			throw new IllegalStateException("File not built: "+cus.iterator().next());
		}
	}
	
	private Set<String> readExpected(File expected) throws IOException {
		final BufferedReader br = new BufferedReader(new FileReader(expected));
		final Set<String> cus   = new HashSet<String>();
		String line;
		while ((line = br.readLine()) != null) {
			cus.add(line.trim());
		}
		return cus;
	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getResource() == null) {
			for(IResourceDelta delta : event.getDelta().getAffectedChildren()) {
				changed(delta);
			}
			return;
		}
		if (!(event.getResource() instanceof IProject)) {
			// Gets changes to the root /
			System.out.println("Ignoring change1 to "+event.getResource());
			return;
		}
		switch (event.getType()) {
		case IResourceChangeEvent.PRE_DELETE:
			//Handled by removal
			//System.out.println("Ignoring deletion of project "+event.getResource().getName());
			break;
		case IResourceChangeEvent.PRE_CLOSE:
			/*Handled below
			if (script != null) {
				printToScript(ScriptCommands.CLOSE_PROJECT+' '+event.getResource().getName());
			}
			*/
			break;
		default:
			System.out.println("Ignoring change2 to "+event.getResource().getName());
			return;
		case IResourceChangeEvent.PRE_BUILD:
			changed(event.getDelta());
		}
	}
	
	private void changed(IResourceDelta delta) {
		if (!(delta.getResource() instanceof IProject)) {
			System.out.println("Ignoring change4 to "+delta.getResource());
			return;
		}
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			System.out.println("Handling addition as a new project "+delta.getResource());
			if (script != null) {
				printToScript(ScriptCommands.CREATE_PROJECT+' '+delta.getResource().getName());
			}
			return;
		case IResourceDelta.REMOVED:
			System.out.println("Handling removal of project "+delta.getResource());
			if (script != null) {
				printToScript(ScriptCommands.DELETE_PROJECT+' '+delta.getResource().getName());
			}
			return;
		case IResourceDelta.CHANGED:
			if (delta.getFlags() != IResourceDelta.OPEN) {
				System.out.println("Ignoring change5 to project "+delta.getResource()+": "+delta.getFlags());
				/*
                for(IResourceDelta d : delta.getAffectedChildren()) {
					changed(d);
				}
				*/
				return;
			}
			final IProject p = (IProject) delta.getResource();
			if (p.isOpen()) {
				System.out.println("Handling opening project "+delta.getResource());
				if (script != null) {
					printToScript(ScriptCommands.OPEN_PROJECT+' '+delta.getResource().getName());
				}
			} else {
				System.out.println("Handling closing project "+delta.getResource());
				if (script != null) {
					printToScript(ScriptCommands.CLOSE_PROJECT+' '+delta.getResource().getName());
				}
			}
			return;
		default:
			System.out.println("Ignoring change3 to "+delta.getResource());
			return;
		}
	}
}
