package com.surelogic.javac;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.IIRProjects;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.tool.SureLogicToolsPropertiesUtility;
import com.surelogic.javac.persistence.JSureProjectsXMLCreator;
import com.surelogic.javac.persistence.PersistenceConstants;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.util.FilterIterator;

public class Projects extends JavaProjects implements IIRProjects, Iterable<JavacProject> {
  public static JavacProject getProject(IRNode cu) {
    return (JavacProject) JavaProjects.getProject(cu);
  }

  static void setProject(IRNode cu, JavacProject p) {
    if (p == null) {
      return;
    }
    /*
     * String name = JavaNames.genPrimaryTypeName(cu); if (name != null) {
     * System.out.println("Marking as in "+p.getName()+": "+JavaNames.
     * genPrimaryTypeName(cu)); }
     */

    // HACK until arrayType is cloned
    JavacProject old = getProject(cu);
    if (old == null || !old.isActive()) {
      if (old != null) {
        System.out.println("Resetting project for " + DebugUnparser.toString(cu));
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
  private final HashMap<Pair<String, File>, CodeInfo> loadedClasses = new HashMap<Pair<String, File>, CodeInfo>();
  private final Date date;
  private final File location;
  private File f_scanDir;
  private File f_resultsFile;

  private static final String UNINIT = "<uninitialized>";
  private String f_scan;
  private String f_previousPartialScan;
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
    f_scan = UNINIT;
    date = d;
  }

  /**
   * Only used by Util and jsure-ant
   */
  public Projects(Config cfg, SLProgressMonitor monitor) {
    f_scan = UNINIT;
    this.monitor = monitor;
    add(cfg);
    location = cfg.getLocation();
    isAuto = false;
    args = new HashMap<String, Object>();
    date = new Date();
  }

  public void computeScan(File dataDir, Projects oldProjects) throws Exception {
    if (f_scan != UNINIT) {
      throw new IllegalStateException("Run already set: " + f_scan);
    }
    if (oldProjects != null) {
      setPreviousPartialScan(oldProjects.f_scan);
    }

    final String time = SLUtility.toStringHMS(getDate());
    final String name = getShortLabel() + ' ' + time.replace(':', '-');
    f_scan = name;
    f_scanDir = new File(dataDir, name);
    f_scanDir.mkdirs();

    final String resultsName = oldProjects != null ? PersistenceConstants.PARTIAL_RESULTS_ZIP : PersistenceConstants.RESULTS_ZIP;
    f_resultsFile = new File(f_scanDir, resultsName);

    // System.out.println("Contents of projects: "+run);
    final File xml = new File(f_scanDir, PersistenceConstants.PROJECTS_XML);
    final PrintStream pw = new PrintStream(xml);
    try {
      JSureProjectsXMLCreator creator = new JSureProjectsXMLCreator(pw);
      // TODO the problem is that I won't know what the last run was until
      // later ...

      creator.write(this);
    } finally {
      pw.close();
    }
  }

  public File getLocation() {
    return location;
  }

  public File getRunDir() {
    return f_scanDir;
  }

  public String getRun() {
    return f_scan;
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
    if (f_scan != UNINIT) {
      throw new IllegalStateException("Adding config after run already set: " + f_scan);
    }
    resetOrdering();
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

  public void resetOrdering() {
    ordering.clear();
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
   * this.projects.values()) { copy.projects.put(jp.getName(), jp.copy(copy)); }
   * for(JavacProject jp : this.ordering) {
   * copy.ordering.add(copy.projects.get(jp.getName())); } copy.run = this.run;
   * return copy; }
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
    if (f_previousPartialScan == null) {
      throw new MergeException("lastRun not already set to " + oldProjects.f_scan);
    } else if (!f_previousPartialScan.equals(oldProjects.f_scan)) {
      throw new MergeException("lastRun doesn't match: " + f_previousPartialScan + " -- " + oldProjects.f_scan);
    }
    /*
     * // TODO Merge options? for(Map.Entry<String,Object> e :
     * oldProjects.options.entrySet()) { if (!options.containsKey(e.getValue()))
     * { options.put(e.getKey(), e.getValue()); } }
     */
    for (JavacProject old : oldProjects.projects.values()) {
      JavacProject newP = projects.get(old.getName());
      if (newP == null) {
        projects.put(old.getName(), old);
        resetOrdering();
      } else {
        // TODO is this right?
        projects.put(newP.getName(), new JavacProject(this, old, newP.getConfig(), monitor));
        resetOrdering();
      }
    }
    // lastRun = oldProjects.run;
    return this;
  }

  /*
   * public void setOption(String key, Object value) { options.put(key, value);
   * }
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

  /**
   * @return the project that the file is in
   */
  public String checkMapping(File f) {
    return fileMap.get(f);
  }

  public boolean isAutoBuild() {
    return isAuto;
  }

  public boolean isDelta() {
    return delta;
  }

  public String getPreviousPartialScan() {
    return f_previousPartialScan;
  }

  public void setPreviousPartialScan(String last) {
    if (last == null) {
      throw new IllegalArgumentException(I18N.err(44, "last"));
    }
    if (f_previousPartialScan != null) {
      throw new IllegalStateException(I18N.err(230, f_previousPartialScan));
    }
    f_previousPartialScan = last;
    delta = last != null;
  }

  /**
   * Reuse state from the last set of projects
   */
  public void init(Projects oldProjects) throws MergeException {
    if (f_previousPartialScan == null) {
      throw new MergeException("lastRun not already set to " + oldProjects.f_scan);
    } else if (!f_previousPartialScan.equals(oldProjects.f_scan)) {
      throw new MergeException("lastRun doesn't match: " + f_previousPartialScan + " -- " + oldProjects.f_scan);
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
    final Pair<String, File> key = Pair.getInstance(ref, src);
    CodeInfo info = loadedClasses.get(key);
    if (info != null && info.getNode().identity() == IRNode.destroyedNode) {
      loadedClasses.remove(key);
      return null;
    }
    return info;
  }

  public void addLoadedClass(String ref, File src, CodeInfo info) {
    // loadedClasses.put(ref, src, info);
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
    return f_resultsFile;
  }

  /**
   * Gets the source folders that were excluded from analysis (relative to the
   * workspace).
   * 
   * @return the excluded source folders, or an empty list if none.
   */
  public List<String> getExcludedSourceFolders() {
    List<String> folders = new ArrayList<String>();
    for (Config c : getConfigs()) {
      String[] here = c.getListOption(SureLogicToolsPropertiesUtility.SCAN_EXCLUDE_SOURCE_FOLDER);
      for (String p : here) {
        folders.add('/' + c.getProject() + '/' + p);
      }
    }
    if (folders.size() == 0) {
      return Collections.emptyList();
    }
    return folders;
  }

  /**
   * Gets the excluded source packages with wildcards.
   * 
   * @return the excluded source package spec with wildcards, or an empty list
   *         if none.
   */
  public List<String> getExcludedSourcePackageSpec() {
    List<String> pkgs = new ArrayList<String>();
    for (Config c : getConfigs()) {
      String[] here = c.getListOption(SureLogicToolsPropertiesUtility.SCAN_EXCLUDE_SOURCE_PACKAGE);
      for (String p : here) {
        pkgs.add(p);
      }
    }
    if (pkgs.size() == 0) {
      return Collections.emptyList();
    }
    return pkgs;
  }

  public String getConciseExcludedFoldersAndPackages() {
    final StringBuilder b = new StringBuilder();

    List<String> flds = getExcludedSourceFolders();
    List<String> pkgs = getExcludedSourcePackageSpec();

    if (!flds.isEmpty()) {
      b.append("Folders: ");
      boolean first = true;
      for (String s : flds) {
        if (first) {
          first = false;
        } else {
          b.append(", ");
        }
        b.append(s);
      }
    }

    if (!pkgs.isEmpty()) {
      if (!flds.isEmpty()) {
        b.append("; ");
      }
      b.append("Packages: ");
      boolean first = true;
      for (String s : pkgs) {
        if (first) {
          first = false;
        } else {
          b.append(", ");
        }
        b.append(s);
      }
    }
    return b.toString();
  }

  /**
   * @return true if they have the same projects
   */
  public boolean matchProjects(Projects other) {
    return projects.keySet().equals(other.projects.keySet());
  }
}
