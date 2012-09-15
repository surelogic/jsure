package com.surelogic.jsure.core.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.IIRProjects;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.BinaryCUDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.PackageDrop;
import com.surelogic.dropsea.ir.drops.ProjectsDrop;
import com.surelogic.dropsea.ir.drops.SourceCUDrop;
import com.surelogic.dropsea.ir.drops.regions.RegionModel;
import com.surelogic.javac.Config;
import com.surelogic.javac.IClassPathEntry;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;
import com.surelogic.jsure.core.driver.JavacDriver;
import com.surelogic.jsure.core.scripting.ScriptCommands;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.adapter.AdapterUtil;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class ClearProjectListener implements IResourceChangeListener {
  public static final boolean clearAfterChange = true;
  private static final List<IProject> lastProjects = new ArrayList<IProject>();

  public ClearProjectListener() {
    // Nothing to do
  }

  @Override
  public void resourceChanged(final IResourceChangeEvent event) {
    synchronized (ClearProjectListener.class) {
      if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
        if (!lastProjects.isEmpty()) {
          clearJSureState(lastProjects);
          lastProjects.clear();
        }
      } else if (event.getResource() instanceof IProject) {
        final IProject p = (IProject) event.getResource();
        if (!isActive(ProjectsDrop.getProjects(), p.getName())) {
          return;
        }
        switch (event.getType()) {
        case IResourceChangeEvent.PRE_CLOSE:
        case IResourceChangeEvent.PRE_DELETE:
          if (clearAfterChange) {
            lastProjects.add(p);
          } else {
            clearJSureState(Collections.singletonList(p));
          }
          return;
        default:
          return;
        }
      }
    }
  }

  private boolean isActive(IIRProjects projs, String name) {
    for (String p : projs.getProjectNames()) {
      if (p.equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Otherwise, clear the current project
   */
  private static final boolean clearAll = true;

  public static void clearJSureState() {
    clearJSureState(null);
  }

  public static void clearJSureState(List<IProject> removedProjects) {
    System.out.println("Clearing JSure state");
    if (JavacDriver.getInstance() != null) {
      JavacDriver.getInstance().recordProjectAction(ScriptCommands.CLEANUP_DROPS_FIRST, removedProjects);
    }
    try {
      synchronized (Sea.getDefault()) {
        clearDropSea(clearAll, removedProjects);
      }
      /*
       * for(Drop d : Sea.getDefault().getDrops()) {
       * System.out.println(d.getMessage()); }
       */
      // System.out.println("Clearing all comp units");
      // Binding.clearCompUnits(clearAll);
      // TypeBindings.clearAll();

      if (clearAll) {
        JavaTypeFactory.clearAll();
        IDE.getInstance().notifyASTsChanged();
        IDE.getInstance().clearAll();
      }
      // Go ahead and garbage collect the IR.
      SlotInfo.gc();
    } catch (final Exception e) {
      SLLogger.getLogger().log(Level.SEVERE, "Problem while clearing JSure state", e);
    }
  }

  private static Set<IClearProjectHelper> helpers = new HashSet<IClearProjectHelper>();

  public static void addHelper(final IClearProjectHelper h) {
    helpers.add(h);
  }

  private static void computeProjectDependencies(Config c, Set<String> needed) {
    if (needed.contains(c.getProject())) {
      return; // Already handled
    }
    needed.add(c.getProject());
    for (IClassPathEntry e : c.getClassPath()) {
      if (e instanceof Config) {
        computeProjectDependencies((Config) e, needed);
      }
    }
  }

  private static boolean debug = false;

  @SuppressWarnings("unused")
  private static void clearDropSea(final boolean clearAll, Iterable<IProject> removed) {
    final ProjectsDrop pd = ProjectsDrop.getDrop();
    if (pd == null) {
      System.out.println("No ProjectsDrop, so nothing to clear");
      return;
    }
    // final List<IProject> removedProjects = new ArrayList<IProject>();
    Set<String> removedNames = null;
    final StringBuilder sb = debug ? new StringBuilder() : null;
    if (removed != null) {
      removedNames = new HashSet<String>();

      for (IProject p : removed) {
        // removedProjects.add(p);
        removedNames.add(p.getName());
        if (debug) {
          if (sb.length() != 0) {
            sb.append(", ");
          }
          sb.append(p.getName());
        }
      }
    }
    // System.out.println("Clearing drop-sea: "+sb);

    // Filter out needed projects
    final Projects oldP = (Projects) pd.getIIRProjects();
    final Set<String> needed;
    if (removed != null) {
      needed = new HashSet<String>();
      for (JavacProject p : oldP) {
        if (!removedNames.contains(p.getName())) {
          computeProjectDependencies(p.getConfig(), needed);
        }
      }
      if (debug) {
        sb.setLength(0);
        for (String s : needed) {
          if (sb.length() != 0) {
            sb.append(", ");
          }
          sb.append(s);
        }
        System.out.println("Still needed: " + sb);
      }
      removedNames.removeAll(needed);
    }

    for (final RegionModel region : Sea.getDefault().getDropsOfExactType(RegionModel.class)) {
      final IRNode n = region.getNode();
      final IRNode root = VisitUtil.findRoot(n);
      if (removed != null && !removedNames.contains(region.getProject())) {
        // It's not part of any project that was removed
        continue;
      }
      final CUDrop drop = CUDrop.queryCU(root);
      if (drop instanceof SourceCUDrop) {
        // System.out.println(region.getMessage());
        region.invalidate();
      }
    }
    //RegionModel.purgeUnusedRegions();

    if (pd != null) {
      final List<IIRProject> removedJps = new ArrayList<IIRProject>();
      for (JavacProject jp : ((Projects) pd.getIIRProjects())) {
        if (removed == null || removedNames.contains(jp.getName())) {
          removedJps.add(jp);
        }
      }
      if (debug) {
        sb.setLength(0);
        for (IIRProject p : removedJps) {
          if (sb.length() != 0) {
            sb.append(", ");
          }
          sb.append(p.getName());
        }
        System.out.println("Removing projects: " + sb);
      }
      for (IRNode node : SourceCUDrop.invalidateAll(removedJps)) {
        //System.out.println("Destroyed: " + cud.getJavaOSFileName());
        AdapterUtil.destroyOldCU(node);
      }

      for (JavacProject jp : oldP) {
        System.out.println("Deactivating " + jp);
        jp.deactivate();
      }
      final Projects newP = oldP.remove(removedNames);
      if (newP == null) {
        // No projects left
        pd.invalidate();
      } else {
        ProjectsDrop.ensureDrop(newP);
      }
    } else {
      // Nuke everything
      for (final IRNode node : SourceCUDrop.invalidateAll(null)) {
        AdapterUtil.destroyOldCU(node);
      }
      if (clearAll) {
        for (final IRNode node : BinaryCUDrop.invalidateAll()) {
          AdapterUtil.destroyOldCU(node);
        }
        PackageDrop.invalidateAll();
        IDE.getInstance().clearAll();
        AnnotationRules.XML_LOG.reset();
      }
    }
    // TODO are these necessary?
    /*
     * Sea.getDefault().invalidateMatching(
     * DropPredicateFactory.matchType(ProjectsDrop.class));
     * Sea.getDefault().invalidateMatching(
     * DropPredicateFactory.matchType(ProjectDrop.class));
     * Sea.getDefault().invalidateMatching(
     * DropPredicateFactory.matchType(WarningDrop.class));
     * Sea.getDefault().invalidateMatching(
     * DropPredicateFactory.matchType(PromiseWarningDrop.class));
     */

    for (final IClearProjectHelper h : helpers) {
      if (h != null) {
        h.clearResults(clearAll);
      }
    }
    // Sea.getDefault().notifySeaObservers();
  }
}
