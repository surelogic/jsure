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

import com.surelogic.analysis.*;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.tool.SureLogicToolsPropertiesUtility;
import com.surelogic.common.util.*;
import com.surelogic.javac.persistence.JSureProjectsXMLCreator;
import com.surelogic.javac.persistence.PersistenceConstants;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class Projects extends JavaProjectSet<JavacProject> implements IIRProjects, Iterable<JavacProject> {
  private static final IRObjectType<IIRProject> projectType = new IRObjectType<IIRProject>();
  protected static final SlotInfo<IIRProject> projectSI = 
		  JavaNode.getVersionedSlotInfo(Projects.class.getName(), projectType);

  public static JavacProject getProject(IRNode cu) {
	  if (cu == null || !cu.valueExists(projectSI)) {
		  return null;
	  }
	  return (JavacProject) cu.getSlotValue(projectSI);
  }

  public static IIRProject getEnclosingProject(IRNode here) {
	  //This doesn't use getPromisedFor
	  //final IRNode cu = VisitUtil.findRoot(here);
	  final IRNode cu = VisitUtil.findCompilationUnit(here);
	  //final IRNode cu = VisitUtil.getEnclosingCompilationUnit(here);
	  return getProject(cu);
  }

  public static IIRProject getEnclosingProject(IJavaType t) {
	  if (t instanceof IJavaDeclaredType) {
		  IJavaDeclaredType dt = (IJavaDeclaredType) t;
		  return getEnclosingProject(dt.getDeclaration());
	  }
	  if (t instanceof IJavaTypeFormal) {
		  IJavaTypeFormal dt = (IJavaTypeFormal) t;
		  return getEnclosingProject(dt.getDeclaration());
	  }
	  return null;
  }
	
  public static void setProject(IRNode cu, IIRProject p) {
	  if (p == null) {
		  return;
	  }
	  /*
		if (p.getName().equals("Util")) {
			String name = JavaNames.genPrimaryTypeName(cu);
			if (name != null) {
				System.out.println("Marking as in Util: "+JavaNames.genPrimaryTypeName(cu));
				System.out.println();
			}
		}
	   */
	  // HACK until arrayType is cloned
	  IIRProject old = getProject(cu);
	  if (old == null) {
		  cu.setSlotValue(projectSI, p);
	  } 
	  else if (old != p) {
		  SLLogger.getLogger().warning("Ignored attempt to reset project from "+old.getName()+" to "+p.getName()+" for "+DebugUnparser.toString(cu));
	  }
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

  private final HashMap<Pair<String, File>, CodeInfo> loadedClasses = new HashMap<Pair<String, File>, CodeInfo>();

  public Projects(File loc, boolean isAuto, Map<String, Object> args) {
    super(loc, isAuto, new Date(), args);
  }

  public Projects(File loc, boolean isAuto, Date d, Map<String, Object> args) {
	super(loc, isAuto, d, args);
  }

  /**
   * Only used by Util and jsure-ant
   */
  public Projects(Config cfg, SLProgressMonitor monitor) {
	 super(cfg, monitor);
  }
  
  public void computeScan(File dataDir, Projects oldProjects) throws Exception {
    if (f_scanDirName != UNINIT) {
      throw new IllegalStateException("Run already set: " + f_scanDirName);
    }
    if (oldProjects != null) {
      setPreviousPartialScan(oldProjects.f_scanDirName);
    }

    final String scanDirName = SLUtility.getScanDirectoryName(getFirstProjectNameAlphaOrNull(), multiProject(), getDate());
    f_scanDirName = scanDirName;
    f_scanDir = new File(dataDir, scanDirName);
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

  public void setMonitor(SLProgressMonitor m) {
    for (JavacProject p : projects.values()) {
      p.getTypeEnv().setProgressMonitor(m);
    }
    monitor = m;
  }

  public JavacProject add(Config cfg) {
    if (f_scanDirName != UNINIT) {
      throw new IllegalStateException("Adding config after run already set: " + f_scanDirName);
    }
    resetOrdering();
    JavacProject p = new JavacProject(this, cfg, cfg.getProject(), monitor);
    projects.put(cfg.getProject(), p);
    return p;
  }

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
      throw new MergeException("lastRun not already set to " + oldProjects.f_scanDirName);
    } else if (!f_previousPartialScan.equals(oldProjects.f_scanDirName)) {
      throw new MergeException("lastRun doesn't match: " + f_previousPartialScan + " -- " + oldProjects.f_scanDirName);
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

  /**
   * Reuse state from the last set of projects
   */
  public void init(Projects oldProjects) throws MergeException {
    if (f_previousPartialScan == null) {
      throw new MergeException("lastRun not already set to " + oldProjects.f_scanDirName);
    } else if (!f_previousPartialScan.equals(oldProjects.f_scanDirName)) {
      throw new MergeException("lastRun doesn't match: " + f_previousPartialScan + " -- " + oldProjects.f_scanDirName);
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
}
