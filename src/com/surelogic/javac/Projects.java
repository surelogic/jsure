package com.surelogic.javac;

import java.io.*;
import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.jobs.*;
import com.surelogic.javac.persistence.*;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
//import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.util.*;

public class Projects extends JavaProjects implements IIRProjects,
		Iterable<JavacProject> {
	public static JavacProject getProject(IRNode cu) {
		return (JavacProject) JavaProjects.getProject(cu);
	}

	static void setProject(IRNode cu, JavacProject p) {
		if (p == null) {
			return;
		}
		/*
		String name = JavaNames.genPrimaryTypeName(cu);
		if (name != null) {
			System.out.println("Marking as in "+p.getName()+": "+JavaNames.genPrimaryTypeName(cu));
		}
		*/
		
		// HACK until arrayType is cloned
		JavacProject old = getProject(cu);
		if (old == null || !old.isActive()) {
			if (old != null) {
				System.out.println("Resetting project for "
						+ DebugUnparser.toString(cu));
			}
			cu.setSlotValue(projectSI, p);
		}
	}

	private SLProgressMonitor monitor;
	// private final Map<String,Object> options = new HashMap<String, Object>();
	private final Map<String, JavacProject> projects = new HashMap<String, JavacProject>();
	private final List<JavacProject> ordering = new ArrayList<JavacProject>();
	// To project names
	private final Map<File, String> fileMap = new HashMap<File, String>();
	private final Hashtable2<String, File, CodeInfo> loadedClasses = new Hashtable2<String, File, CodeInfo>();
	private final Date date;
	private final File location;
	private File runDir, resultsFile;

	private static final String UNINIT = "<uninitialized>";
	private String run;
	private String lastRun;
	private boolean delta = false;
	private final boolean isAuto;
	private final Map<String, Object> args;

	public Projects(File loc, boolean isAuto, Map<String, Object> args) {
		this(loc, isAuto, new Date(), args);
	}
	
	public Projects(File loc, boolean isAuto, Date d, Map<String, Object> args) {
		location = loc;
		this.isAuto = isAuto;
		this.args = args;
		run = UNINIT;
		date = d;
	}

	/**
	 * Only used by Util and jsure-ant
	 */
	public Projects(Config cfg, SLProgressMonitor monitor) {
		this.monitor = monitor;
		add(cfg);
		location = cfg.getLocation();
		isAuto = false;
		args = new HashMap<String, Object>();
		run = null;
		date = new Date();
	}

	public void computeRun(File dataDir, Projects oldProjects) throws Exception {
		if (run != UNINIT) {
			throw new IllegalStateException("Run already set: " + run);
		}
		if (oldProjects != null) {
			setLastRun(oldProjects.run);
		}

		final String time = SLUtility.toStringHMS(getDate());
		final String name = getShortLabel() + ' ' + time.replace(':', '-');
		run = name;
		runDir = new File(dataDir, name);
		runDir.mkdirs();

		final String resultsName = oldProjects != null ? PersistenceConstants.PARTIAL_RESULTS_ZIP
				: PersistenceConstants.RESULTS_ZIP;
		resultsFile = new File(runDir, resultsName);

		// System.out.println("Contents of projects: "+run);
		final File xml = new File(runDir, PersistenceConstants.PROJECTS_XML);
		final PrintStream pw = new PrintStream(xml);
		try {
			JSureProjectsXMLCreator creator = new JSureProjectsXMLCreator(pw);
			// TODO the problem is that I won't know what the last run was until
			// later ...

			creator.write(this);
		} finally {
			pw.close();
		}
		// PromiseMatcher.load(runDir);
	}

	public File getLocation() {
		return location;
	}

	public File getRunDir() {
		return runDir;
	}

	public String getRun() {
		return run;
	}

	public Date getDate() {
		return date;
	}

	public int size() {
		return projects.size();
	}

	public String getLabel() {
		final StringBuilder sb = new StringBuilder();
		for (JavacProject p : projects.values()) {
			if (p.getConfig().getBoolOption(Config.AS_SOURCE)) {
				if (sb.length() != 0) {
					sb.append(", ");
				}
				sb.append(p.getName());
			}
		}
		return sb.toString();
	}

	public String getShortLabel() {
		String l = getLabel();
		if (l.length() > 100) {
			l = l.substring(0, 100);
		}
		return l;
	}

	public void setMonitor(SLProgressMonitor m) {
		for (JavacProject p : projects.values()) {
			p.getTypeEnv().setProgressMonitor(m);
		}
		monitor = m;
		IDE.getInstance().setDefaultClassPath(getProject());
	}

	public SLProgressMonitor getMonitor() {
		return monitor;
	}

	public JavacProject get(String name) {
		return projects.get(name);
	}

	public boolean contains(String name) {
		return get(name) != null;
	}

	public JavacProject add(Config cfg) {
		if (run != UNINIT) {
			throw new IllegalStateException(
					"Adding config after run already set");
		}
		ordering.clear();
		JavacProject p = new JavacProject(this, cfg, cfg.getProject(), monitor);
		projects.put(cfg.getProject(), p);
		return p;
	}

	public Iterable<Config> getConfigs() {
		return new FilterIterator<JavacProject, Config>(iterator()) {
			@Override
			protected Object select(JavacProject p) {
				return p.getConfig();
			}
		};
	}

	public Iterable<String> getProjectNames() {
		return new FilterIterator<JavacProject, String>(iterator()) {
			@Override
			protected Object select(JavacProject p) {
				return p.getName();
			}
		};
	}

	public Iterable<? extends IIRProject> getProjects() {
		populateOrdering();
		return ordering;
	}

	public Iterator<JavacProject> iterator() {
		populateOrdering();
		return ordering.iterator();
	}

	private void populateOrdering() {
		if (ordering.isEmpty()) {
			// Populate the ordering
			for (JavacProject p : projects.values()) {
				populateOrdering(p);
			}
		}
	}

	private void populateOrdering(final JavacProject p) {
		if (p != null && !ordering.contains(p)) {
			for (Config c : p.getConfig().getDependencies()) {
				if (c != p.getConfig()) {
					JavacProject jp = get(c.getProject());
					populateOrdering(jp);
				}
			}
			ordering.add(p);
		}
	}

	/*
	 * void setTypeEnv(JavacTypeEnvironment te) { if (te != null) { tEnv = te;
	 * tEnv.setProgressMonitor(monitor);
	 * 
	 * // HACK for now boolean first = true; for(JavacProject p :
	 * projects.values()) { p.setTypeEnv(te); if (first) {
	 * IDE.getInstance().setDefaultClassPath(p); first = false; } } } }
	 */

	/**
	 * Create a new Projects, removing the specified projects
	 */
	public Projects remove(Collection<String> removed) {
		if (removed == null) {
			return null;
		}
		if (!XUtil.testing) {
			final Iterator<String> it = removed.iterator();
			while (it.hasNext()) {
				String name = it.next();
				if (get(name) == null) {
					// eliminate projects that don't exist
					System.err.println("No such project: " + name);
					it.remove();
				}
			}
		}
		if (removed.isEmpty()) {
			return this;
		}
		Projects p = new Projects(location, isAuto, args);
		for (JavacProject old : projects.values()) {
			if (!removed.contains(old.getName())) {
				p.projects.put(old.getName(), old);
			}
		}
		if (p.projects.isEmpty()) {
			return null;
		}
		return p;
	}

	/*
	 * Projects copy() { final Projects copy = new Projects(isAuto);
	 * copy.fileMap.putAll(this.fileMap);
	 * copy.loadedClasses.putAll(this.loadedClasses); for(JavacProject jp :
	 * this.projects.values()) { copy.projects.put(jp.getName(), jp.copy(copy));
	 * } for(JavacProject jp : this.ordering) {
	 * copy.ordering.add(copy.projects.get(jp.getName())); } copy.run =
	 * this.run; return copy; }
	 */

	public boolean conflictsWith(Projects oldProjects) {
		for (JavacProject old : oldProjects.projects.values()) {
			JavacProject newP = projects.get(old.getName());
			if (newP != null) {
				if (newP.conflictsWith(old)) {
					return true;
				}
			}
		}
		return false;
	}

	public Projects merge(Projects oldProjects) throws MergeException {
		if (oldProjects == null) {
			return this;
		}
		if (lastRun == null) {
			throw new MergeException("lastRun not already set to "
					+ oldProjects.run);
		} else if (!lastRun.equals(oldProjects.run)) {
			throw new MergeException("lastRun doesn't match: " + lastRun
					+ " -- " + oldProjects.run);
		}
		/*
		 * // TODO Merge options? for(Map.Entry<String,Object> e :
		 * oldProjects.options.entrySet()) { if
		 * (!options.containsKey(e.getValue())) { options.put(e.getKey(),
		 * e.getValue()); } }
		 */
		for (JavacProject old : oldProjects.projects.values()) {
			JavacProject newP = projects.get(old.getName());
			if (newP == null) {
				projects.put(old.getName(), old);
			} else {
				// TODO is this right?
				projects.put(newP.getName(),
						new JavacProject(this, old, newP.getConfig(), monitor));
			}
		}
		// lastRun = oldProjects.run;
		return this;
	}

	/*
	 * public void setOption(String key, Object value) { options.put(key,
	 * value); }
	 * 
	 * public int getIntOption(String key) { Integer i = (Integer)
	 * options.get(key); return i != null ? i : 0; }
	 */


	public JavacProject getProject() {
		for (JavacProject p : projects.values()) {
			return p;
		}
		// TODO Auto-generated method stub
		return null;
	}

	public void mapToProject(File f, String project) {
		fileMap.put(f, project);
	}

	public String checkMapping(File f) {
		return fileMap.get(f);
	}

	public boolean isAutoBuild() {
		return isAuto;
	}

	public boolean isDelta() {
		return delta;
	}

	public String getLastRun() {
		return lastRun;
	}

	public void setLastRun(String last) {
		if (last == null) {
			throw new IllegalArgumentException("lastRun cannot be set to null");
		}
		if (lastRun != null) {
			throw new IllegalStateException("lastRun already set to " + lastRun);
		}
		lastRun = last;
		delta = last != null;
	}

	/**
	 * Reuse state from the last set of projects
	 */
	public void init(Projects oldProjects) throws MergeException {
		if (lastRun == null) {
			throw new MergeException("lastRun not already set to "
					+ oldProjects.run);
		} else if (!lastRun.equals(oldProjects.run)) {
			throw new MergeException("lastRun doesn't match: " + lastRun
					+ " -- " + oldProjects.run);
		}
		for (JavacProject jp : projects.values()) {
			JavacProject old = oldProjects.projects.get(jp.getName());
			if (old != null) {
				jp.init(old);
			}
		}
		loadedClasses.putAll(oldProjects.loadedClasses);
		setMonitor(monitor);
		delta = true;
	}

	public CodeInfo getLoadedClasses(String ref, File src) {
		CodeInfo info = loadedClasses.get(ref, src);
		if (info != null && info.getNode().identity() == IRNode.destroyedNode) {
			loadedClasses.remove(ref, src);
			return null;
		}
		return info;
	}

	public void addLoadedClass(String ref, File src, CodeInfo info) {
		//loadedClasses.put(ref, src, info);
	}

	public Object getArg(String key) {
		return args.get(key);
	}

	public void setArg(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("Null key");
		}
		args.put(key, value);
	}

	public File getResultsFile() {
		return resultsFile;
	}
}
