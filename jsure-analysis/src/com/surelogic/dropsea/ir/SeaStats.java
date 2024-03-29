package com.surelogic.dropsea.ir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.ir.drops.AssumePromiseDrop;
import com.surelogic.dropsea.ir.drops.VouchPromiseDrop;

public final class SeaStats {
  private SeaStats() {
    // Nothing to do
  }

  public interface Splitter<T> {
    T getLabel(IDrop d);
  }

  public static final <T> Map<T, Set<IDrop>> split(Collection<? extends IDrop> d, Splitter<T> split) {
    final Map<T, Set<IDrop>> rv = new HashMap<>();
    for (IDrop i : d) {
      final T label = split.getLabel(i);
      if (label == null) {
        continue;
      }
      Set<IDrop> labelled = rv.get(label);
      if (labelled == null) {
        labelled = new HashSet<>();
        rv.put(label, labelled);
      }
      labelled.add(i);
    }
    return rv;
  }

  public interface Counter {
    String count(IDrop d);
  }

  public static final Map<String, Integer> count(Collection<? extends IDrop> d, Counter[] counters) {
    Map<String, Integer> counts = new HashMap<>();
    for (IDrop i : d) {
      for (Counter c : counters) {
        String label = c.count(i);
        if (label != null) {
          Integer count = counts.get(label);
          if (count == null) {
            counts.put(label, 1);
          } else {
            counts.put(label, count + 1);
          }
        }
      }
    }
    return counts;
  }

  public static final <T> Map<T, Map<String, Integer>> count(Map<T, Set<IDrop>> info, Counter[] counters) {
    Map<T, Map<String, Integer>> rv = new HashMap<>();
    for (Map.Entry<T, Set<IDrop>> e : info.entrySet()) {
      rv.put(e.getKey(), count(e.getValue(), counters));
    }
    return rv;
  }

  public static final String PROMISES = "Promises";
  public static final String CONSISTENT = "Consistent";
  public static final String INCONSISTENT = "Inconsistent";
  public static final String VOUCHES = "Vouches";
  public static final String ASSUMES = "Assumes";
  public static final String INFO = "Info";
  public static final String WARNING = "Warnings";
  /*
   * What else is interesting?
   * 
   * public static final String CONSISTENT = "Consistent"; public static final
   * String CONSISTENT = "Consistent";
   */

  static final Map<String, String> labelMap = new HashMap<>();

  static {
    labelMap.put(VouchPromiseDrop.class.getName(), VOUCHES);
    labelMap.put(AssumePromiseDrop.class.getName(), ASSUMES);
    labelMap.put(HintDrop.class.getName(), INFO);
    // labelMap.put(WarningDrop.class.getName(), WARNING);
  }

  public static final Splitter<String> splitByProject = new Splitter<String>() {
    @Override
    public String getLabel(IDrop d) {
      IJavaRef sr = d.getJavaRef();
      if (sr != null) {
        /*
         * String path = sr.getRelativePath(); int firstSlash =
         * path.indexOf('/'); if (firstSlash >= 0 && path.endsWith(".java")) {
         * String label = path.substring(0, firstSlash); if
         * (!label.equals(sr.getProject())) { throw new IllegalStateException(
         * "Mismatched project: "+sr.getProject()); } return label; }
         */
        return sr.getEclipseProjectName();
      }
      return null;
    }
  };

  public static final Counter[] STANDARD_COUNTERS = { new Counter() {
    @Override
    public String count(IDrop d) {
      final IJavaRef sr = d.getJavaRef();
      if (sr == null || sr.getWithin() != IJavaRef.Within.JAVA_FILE) {
        return null; // Not from source
      }
      String l = labelMap.get(d.getFullClassName());
      if (l != null) {
        return l;
      }
      if (d instanceof IPromiseDrop) {
        return PROMISES;
      } else if (d instanceof IResultDrop) {
        IResultDrop pd = (IResultDrop) d;
        return pd.isConsistent() ? CONSISTENT : INCONSISTENT;
      }
      return null;
    }
  } };

  public static final String ALL_PROJECTS = "all projects";

  public static void createSummaryZip(File zip, Collection<? extends IDrop> info, Splitter<String> split, Counter[] counters)
      throws IOException {
    final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
    try {
      final ZipEntry totals = new ZipEntry(ALL_PROJECTS);
      out.putNextEntry(totals);
      for (Map.Entry<String, Integer> e : count(info, counters).entrySet()) {
        final String line = e.getKey() + "=" + e.getValue();
        out.write(line.getBytes());
        out.write('\n');
      }

      for (Map.Entry<String, Map<String, Integer>> e : count(split(info, split), counters).entrySet()) {
        final ZipEntry anEntry = new ZipEntry(e.getKey());
        System.out.println("Project " + e.getKey());
        out.putNextEntry(anEntry);

        for (Map.Entry<String, Integer> e2 : e.getValue().entrySet()) {
          final String line = e2.getKey() + "=" + e2.getValue();
          System.out.println("\t" + line);
          out.write(line.getBytes());
          out.write('\n');
        }
      }
    } finally {
      out.close();
    }
  }
}
