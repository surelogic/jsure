package com.surelogic.javac.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.common.NullOutputStream;
import com.surelogic.common.SourceZipLookup.Lines;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.irfree.CategoryMatcher;
import com.surelogic.dropsea.irfree.DefaultCategoryMatcher;
import com.surelogic.dropsea.irfree.IDropFilter;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.dropsea.irfree.SeaSnapshotDiff;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.util.CPair;

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
  private final SeaSnapshot f_loader;

  public JSureScanInfo(JSureScan run) {
    this(run, null);
  }

  public JSureScanInfo(JSureScan run, SeaSnapshot s) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    f_run = run;
    f_loader = s;
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
      SLLogger.getLogger().log(Level.SEVERE, "Exception trying to getProjects from " + f_run.getDirName(), e);
    }
    return null;
  }

  @NonNull
  private synchronized List<IDrop> loadOrGetDropInfo() {
    if (f_dropInfo != null) {
      return f_dropInfo;
    }
    final long start = System.currentTimeMillis();
    System.out.print("loading IR-free dropsea for " + f_run);
    try {
      if (skipLoading) {
        throw new Exception("Skipping loading");
      }
      f_dropInfo = SeaSnapshot.loadSnapshot(f_loader, f_run.getResultsFile());
      final long end = System.currentTimeMillis();
      System.out.println(" (in " + (end - start) + " ms)");
    } catch (Exception e) {
      System.out.println(" (FAILED)");
      SLLogger.getLogger().log(Level.WARNING, "general failure loading all drops from a snapshot of drop-sea", e);
    }
    if (f_dropInfo == null)
      f_dropInfo = Collections.emptyList();
    return f_dropInfo;
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

  public List<IDrop> getDropInfo() {
    return loadOrGetDropInfo();
  }

  @NonNull
  public <T extends IDrop> Set<T> getDropsOfType(Class<? extends T> dropType) {
    List<IDrop> info = loadOrGetDropInfo();
    if (!info.isEmpty()) {
      final Set<T> result = new HashSet<T>();
      for (IDrop i : info) {
        if (i.instanceOfIRDropSea(dropType)) {
          @SuppressWarnings("unchecked")
          final T i1 = (T) i;
          result.add(i1);
        }
      }
      return result;
    }
    return Collections.emptySet();
  }

  @NonNull
  public ArrayList<IProofDrop> getProofDrops() {
    final ArrayList<IProofDrop> result = new ArrayList<IProofDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IProofDrop) {
        final IProofDrop ipd = (IProofDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  @NonNull
  public ArrayList<IPromiseDrop> getPromiseDrops() {
    final ArrayList<IPromiseDrop> result = new ArrayList<IPromiseDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IPromiseDrop) {
        final IPromiseDrop ipd = (IPromiseDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  @NonNull
  public ArrayList<IProposedPromiseDrop> getProposedPromiseDrops() {
    final ArrayList<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IProposedPromiseDrop) {
        final IProposedPromiseDrop ipd = (IProposedPromiseDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  @NonNull
  public ArrayList<IResultDrop> getResultDrops() {
    final ArrayList<IResultDrop> result = new ArrayList<IResultDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IResultDrop) {
        final IResultDrop ipd = (IResultDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  @NonNull
  public ArrayList<IHintDrop> getHintDrops() {
    final ArrayList<IHintDrop> result = new ArrayList<IHintDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IHintDrop) {
        final IHintDrop ipd = (IHintDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  @NonNull
  public ArrayList<IMetricDrop> getMetricDrops() {
    final ArrayList<IMetricDrop> result = new ArrayList<IMetricDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IMetricDrop) {
        final IMetricDrop ipd = (IMetricDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  @NonNull
  public ArrayList<IModelingProblemDrop> getModelingProblemDrops() {
    final ArrayList<IModelingProblemDrop> result = new ArrayList<IModelingProblemDrop>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IModelingProblemDrop) {
        final IModelingProblemDrop ipd = (IModelingProblemDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  public synchronized String findProjectsLabel() {
    final Projects p = getProjects();
    return p != null ? getProjects().getLabel() : null;
  }

  public SeaSnapshotDiff<CPair<String, String>> diff(JSureScanInfo older, IDropFilter f) {
    SeaSnapshotDiff<CPair<String, String>> rv = new SeaSnapshotDiff<CPair<String, String>>(false ? System.out
        : NullOutputStream.out);
    rv.setFilter(SeaSnapshotDiff.augmentDefaultFilter(f));
    rv.setSeparator(SeaSnapshotDiff.defaultSeparator);
    rv.setMatcher(makeMatcher(older));
    rv.build(older.getDropInfo(), getDropInfo());
    return rv;
  }

  private CategoryMatcher makeMatcher(JSureScanInfo older) {
    try {
      // collect source info
      final Lines newerSrc = new Lines(getJSureRun().getSourceZips());
      final Lines olderSrc = new Lines(older.getJSureRun().getSourceZips());
      return new DefaultCategoryMatcher() {

      };
    } catch (IOException e) {
      return SeaSnapshotDiff.defaultMatcher;
    }
  }
}
