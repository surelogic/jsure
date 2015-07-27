package com.surelogic.java.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.common.NullOutputStream;
import com.surelogic.common.SLUtility;
import com.surelogic.common.SourceZipLookup.Lines;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.java.JavaProjectSet;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.IDecl.Kind;
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
import com.surelogic.dropsea.irfree.SeaSnapshotDiff;
import com.surelogic.dropsea.irfree.drops.SeaSnapshotXMLReader;

import edu.cmu.cs.fluid.util.CPair;
import edu.cmu.cs.fluid.util.IntegerTable;

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
  private final ConcurrentMap<String, IJavaRef> f_loader;

  public JSureScanInfo(JSureScan run) {
    this(run, null);
  }

  public JSureScanInfo(JSureScan run, ConcurrentMap<String, IJavaRef> cache) {
    if (run == null)
      throw new IllegalArgumentException(I18N.err(44, "run"));
    f_run = run;
    f_loader = cache;
  }

  public synchronized JSureScan getJSureRun() {
    return f_run;
  }

  public synchronized JavaProjectSet<? extends JavaProject> getProjects() {
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
      f_dropInfo = SeaSnapshotXMLReader.loadSnapshot(f_loader, f_run.getResultsFile());
      final long end = System.currentTimeMillis();
      System.out.println(" (in " + SLUtility.toStringDurationMS(end - start, TimeUnit.MILLISECONDS) + ")");

      // Used to precompute properties
      ScanProperty.getScanProperties(f_run.getDir(), this, REQUIRED_PROPS);

      if (printBadLocks) {
        new BadLockInfo().print();
      }
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

  public boolean contains(IDrop drop) {
    return loadOrGetDropInfo().contains(drop);
  }

  @NonNull
  public <T extends IDrop> Set<IDrop> getDropsWithSimpleName(String simpleName) {
    List<IDrop> info = loadOrGetDropInfo();
    if (!info.isEmpty()) {
      final Set<IDrop> result = new HashSet<>();
      for (IDrop i : info) {
        if (i.getSimpleClassName().equals(simpleName)) {
          final IDrop i1 = i;
          result.add(i1);
        }
      }
      return result;
    }
    return Collections.emptySet();
  }

  @NonNull
  public ArrayList<IProofDrop> getProofDrops() {
    final ArrayList<IProofDrop> result = new ArrayList<>();
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
    final ArrayList<IPromiseDrop> result = new ArrayList<>();
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
    final ArrayList<IProposedPromiseDrop> result = new ArrayList<>();
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
    final ArrayList<IResultDrop> result = new ArrayList<>();
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
    final ArrayList<IHintDrop> result = new ArrayList<>();
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
    final ArrayList<IMetricDrop> result = new ArrayList<>();
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
    final ArrayList<IModelingProblemDrop> result = new ArrayList<>();
    for (IDrop i : loadOrGetDropInfo()) {
      if (i instanceof IModelingProblemDrop) {
        final IModelingProblemDrop ipd = (IModelingProblemDrop) i;
        result.add(ipd);
      }
    }
    return result;
  }

  public synchronized String findProjectsLabel() {
    final JavaProjectSet<? extends JavaProject> p = getProjects();
    return p != null ? p.getLabel() : null;
  }

  public SeaSnapshotDiff<CPair<String, String>> diff(JSureScanInfo older, IDropFilter f) {
    SeaSnapshotDiff<CPair<String, String>> rv = new SeaSnapshotDiff<>(false ? System.out : NullOutputStream.out);
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

  /**
   * Produces a number of properties
   */
  private static final ScanProperty<JSureScanInfo> DROP_DEMOGRAPHICS = new ScanProperty<JSureScanInfo>("drop.demographics") {
    @Override
    Iterable<Map.Entry<String, Object>> computeValues(JSureScanInfo s) {
      Map<String, Object> type2number = new HashMap<>();
      type2number.put(key, "computed");
      for (IDrop d : s.getDropInfo()) {
        final String type = "drop." + d.getSimpleClassName();
        Object value = type2number.get(type);
        if (value == null) {
          type2number.put(type, IntegerTable.newInteger(1));
        } else {
          Integer i = (Integer) value;
          type2number.put(type, i + 1);
        }
      }
      return type2number.entrySet();
    }
  };

  @SuppressWarnings("unchecked")
  private static final List<ScanProperty<JSureScanInfo>> REQUIRED_PROPS = SLUtility.list(DROP_DEMOGRAPHICS);

  public static final boolean printBadLocks = false;

  /**
   * For aiding prioritization of bad lock decls
   */
  class BadLockInfo {
    private final Set<IPromiseDrop> regionModels = new HashSet<>();
    private final IPromiseDrop INSTANCE;
    private final Set<IPromiseDrop> INSTANCE_ONLY;

    BadLockInfo() {
      IPromiseDrop instance = null;
      for (IDrop d : getDropsWithSimpleName("RegionModel")) {
        final IPromiseDrop reg = (IPromiseDrop) d;
        regionModels.add(reg);
        // if ("Region(\"public Instance extends All\") on
        // Object".equals(reg.getMessage())) {
        if (reg.getMessage().contains("Instance extends All")) {
          instance = reg;
        }
      }
      INSTANCE = instance;
      INSTANCE_ONLY = Collections.singleton(INSTANCE);
    }

    private Iterable<IPromiseDrop> getRegionModels() {
      return regionModels;
    }

    void print() {
      final List<IPromiseDrop> models = new ArrayList<>();
      for (final IDrop d : getDropsWithSimpleName("LockModel")) {
        final IPromiseDrop lm = (IPromiseDrop) d;
        models.add(lm);
      }
      Collections.sort(models, new Comparator<IPromiseDrop>() {
        @Override
        public int compare(IPromiseDrop o1, IPromiseDrop o2) {
          int rv = o1.getJavaRef().getTypeNameFullyQualified().compareTo(o2.getJavaRef().getTypeNameFullyQualified());
          if (rv == 0) {
            return o1.getMessage().compareTo(o2.getMessage());
          }
          return rv;
        }
      });
      for (final IPromiseDrop lm : models) {
        if (!lm.provedConsistent()) {
          boolean nonEmpty = false;
          if (lm.getMessage().startsWith("PolicyLock")) {
            continue;
          }
          System.out.println(lm.getMessage() + " -- " + lm.getJavaRef().getPackageName());
          for (IPromiseDrop pf : findProtectedFields(lm)) {
            if (pf == null) {
              continue;
            }
            System.out.println("\t" + pf.getMessage() + " : " + pf.getJavaRef().getDeclaration().getTypeOf().getCompact());
            nonEmpty = true;
          }
          if (nonEmpty) {
            System.out.println();
          } else {
            System.out.println();
            // System.out.println("\tNothing found?\n");
          }
        }
      }
      System.out.println();
    }

    private Set<IPromiseDrop> findProtectedFields(IPromiseDrop lm) {
      final IPromiseDrop protectedRegion = findDependentRegionModel(lm);
      String guardedByRegion = null;
      if (lm.getMessage().contains("protects State$_")) {
        int start = lm.getMessage().indexOf("State$_");
        int end = lm.getMessage().indexOf("\")", start);
        guardedByRegion = lm.getMessage().substring(start, end);
      }
      if (protectedRegion == null) {
        if (lm.getMessage().contains("protects Instance\")")) {
          return findContainedFields(lm.getJavaRef().getDeclaration());
        }
        System.out.println("No region models:");
        for (IPromiseDrop dep : lm.getDependentPromises()) {
          System.out.println("\t" + dep.getMessage());
        }
        return Collections.emptySet();
      }
      if (isDeclaredOnField(protectedRegion)) {
        return Collections.singleton(protectedRegion);
      }
      if (protectedRegion == INSTANCE) {
        // A lot of regions otherwise connect to Instance
        /*
         * final String search = lm.getJavaRef().getDeclaration().getName();
         * System.out.println("\tRegions containing: "+search); for(IDrop d :
         * getRegionModels()) { if (d.getMessage().contains(search)) {
         * System.out.println("\t"+d.getMessage()); } }
         */
        return findContainedFields(lm.getJavaRef().getDeclaration());
      }
      // Find fields within protectedRegion
      final Set<IPromiseDrop> set = new HashSet<IPromiseDrop>();
      for (IDrop d : getRegionModels()) {
        final IPromiseDrop reg = (IPromiseDrop) d;
        if (isDeclaredOnField(reg)) {
          if (isEnclosedBy(reg, protectedRegion)) {
            set.add(reg);
          }
        }
      }
      /*
       * if (set.isEmpty()) { final String search =
       * lm.getJavaRef().getDeclaration().getName(); //"extends "
       * +guardedByRegion; for(IDrop d : getRegionModels()) { if
       * (d.getMessage().contains(search)) {
       * System.out.println("\t"+d.getMessage()); } } }
       */
      return set;
    }

    /**
     * Find the fields inside the decl
     */
    // Note: doesn't handle fields in subclasses
    private Set<IPromiseDrop> findContainedFields(final IDecl cdecl) {
      final Set<IPromiseDrop> set = new HashSet<>();
      for (IPromiseDrop reg : getRegionModels()) {
        // if (isDeclaredOnField(reg)) {
        final IDecl loc = reg.getJavaRef().getDeclaration();
        final IDecl parent = loc.getParent();
        if (cdecl.isSameDeclarationAsSloppy(parent)) {
          set.add(reg);
        } else if (parent.getName().equals(cdecl.getName())) {
          System.out.println("Almost ...");
        }
        // }
      }
      return set;
      // return INSTANCE_ONLY;
    }

    private boolean isDeclaredOnField(IPromiseDrop pd) {
      try {
        return pd.getJavaRef().getDeclaration().getKind() == Kind.FIELD;
      } catch (NullPointerException e) {
        return false;
      }
    }

    private IPromiseDrop findDependentRegionModel(IPromiseDrop lm) {
      for (IPromiseDrop dep : lm.getDependentPromises()) {
        if (dep.getFullClassName().equals("com.surelogic.dropsea.ir.drops.RegionModel")) {
          return dep;
        }
      }
      return null;
    }

    private boolean isEnclosedBy(final IPromiseDrop reg, final IPromiseDrop protectedRegion) {
      final IPromiseDrop enclosingRegion = findDependentRegionModel(reg);
      if (protectedRegion == enclosingRegion) {
        return true;
      }
      if (enclosingRegion != null) {
        return isEnclosedBy(enclosingRegion, protectedRegion);
      }
      return false;
    }
  }

  public void clear() {
    f_dropInfo = null;
    f_loader.clear();
    f_run.clear();
  }
}
