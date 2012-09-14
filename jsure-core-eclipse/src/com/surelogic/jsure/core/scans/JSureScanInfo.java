package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.tool.ToolProperties;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.drops.ProjectsDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.javac.Projects;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.javac.persistence.JSureScan;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * Manages the project information, the loading of drop information and other
 * statistics about a particular JSure scan on the disk.
 */
public class JSureScanInfo {
  /**
   * For testing of this class. Used to test how long the loading takes.
   */
  private static final boolean skipLoading = false;

  private List<IDrop> f_dropInfo = null;

  private final JSureScan f_run; // non-null

  public JSureScanInfo(JSureScan run) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    f_run = run;
  }

  public synchronized JSureScan getJSureRun() {
    return f_run;
  }

  public synchronized Projects getProjects() {
    try {
      if (f_run == null) {
        return null;
      }
      return f_run.getProjects();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  private synchronized List<IDrop> loadOrGetDropInfo() {
    if (f_dropInfo != null) {
      return f_dropInfo;
    }
    final long start = System.currentTimeMillis();
    System.out.println("Loading info at " + start);
    try {
      if (skipLoading) {
        throw new Exception("Skipping loading");
      }
      f_dropInfo = SeaSnapshot.loadSnapshot(new File(f_run.getDir(), RemoteJSureRun.RESULTS_XML));
      filterResults();
      final long end = System.currentTimeMillis();
      System.out.println("Finished loading info = " + (end - start) + " ms");
    } catch (Exception e) {
      e.printStackTrace(); // TODO
      f_dropInfo = Collections.emptyList();
    }
    return f_dropInfo;
  }

  private void filterResults() {
    final List<String> folders = getProjects().getExcludedSourceFolders();
    final List<String> packages = getProjects().getExcludedSourcePackageSpec();
    if (folders.isEmpty() && packages.isEmpty()) {
      // Nothing to do
      return;
    }
    // Make the folders match the format for the relative paths
    for (int i = 0; i < folders.size(); i++) {
      String f = folders.get(i);
      if (f.startsWith("/")) {
        folders.set(i, f.substring(1));
      }
    }
    final Pattern[] excludePatterns = ToolProperties.makePackageMatchers(packages.toArray(new String[packages.size()]));
    final Iterator<IDrop> it = f_dropInfo.iterator();
    outer: while (it.hasNext()) {
      final IDrop d = it.next();
      final ISrcRef sr = d.getSrcRef();
      if (sr == null) {
        continue outer;
      }
      for (String f : folders) {
        String path = sr.getRelativePath();

        if (path.startsWith(f)) {
          it.remove();
          continue outer;
        }
      }
      for (Pattern p : excludePatterns) {
        if (p.matcher(sr.getPackage()).matches()) {
          it.remove();
          continue outer;
        }
      }
    }
  }

  public synchronized File getDir() {
    return f_run.getDir();
  }

  public synchronized String getLabel() {
    return f_run.getDir().getName();
  }

  public synchronized boolean isEmpty() {
    return loadOrGetDropInfo().isEmpty();
  }

  public synchronized List<IDrop> getDropInfo() {
    return loadOrGetDropInfo();
  }

  public synchronized boolean dropsExist(Class<? extends Drop> type) {
    for (IDrop i : loadOrGetDropInfo()) {
      if (i.instanceOf(type)) {
        return true;
      }
    }
    return false;
  }

  public synchronized <T extends IDrop, T2 extends Drop> Set<T> getDropsOfType(Class<T2> dropType) {
    List<IDrop> info = loadOrGetDropInfo();
    if (!info.isEmpty()) {
      final Set<T> result = new HashSet<T>();
      for (IDrop i : info) {
        if (i.instanceOf(dropType)) {
          @SuppressWarnings("unchecked")
          final T i1 = (T) i;
          result.add(i1);
        }
      }
      return result;
    }
    return Collections.emptySet();
  }

  public synchronized List<IProofDrop> getProofDropInfo() {
    final List<IProofDrop> result = new ArrayList<IProofDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IProofDrop) {
        final IProofDrop ipd = (IProofDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  public synchronized String findProjectsLabel() {
    for (IDrop info : getDropsOfType(ProjectsDrop.class)) {
      return info.getAttribute(AbstractXMLReader.PROJECTS);
    }
    return null;
  }
}
