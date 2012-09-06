/*
 * Created on Dec 2, 2004
 *
 */
package edu.cmu.cs.fluid.sea.reporting;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;

/**
 * Code initially extracted out of the COE ResultsView
 * 
 * @author Edwin
 * 
 */
public class Report {
  private static String computeKey(Drop drop) {
    String key = drop.getClass().toString().intern();
    if (drop instanceof ResultDrop) {
      key = (key + (((ResultDrop) drop).isConsistent() ? "_PLUS" : "_REDX")).intern();
    }
    return key;
  }

  private static void updateCountMap(Map<String, Integer> droptypeToCount, String key, int inc) {
    if (droptypeToCount.containsKey(key)) {
      int count = droptypeToCount.get(key).intValue() + inc;
      droptypeToCount.put(key, Integer.valueOf(count));
    } else {
      droptypeToCount.put(key, Integer.valueOf(inc));
    }
  }

  public static Map<String, Integer> generateDropCounts() {
    Set<? extends Drop> drops = Sea.getDefault().getDropsOfType(Drop.class);
    // @SuppressWarnings("unchecked")
    // Set<Drop> drops = Sea.getDefault().getDropsOfType(Drop.class);
    return generateDropCounts(drops);
  }

  public static Map<String, Integer> generateDropCounts(Set<? extends Drop> drops) {
    if (drops.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<String, Integer> droptypeToCount = new HashMap<String, Integer>();

    for (Drop drop : drops) {
      if (drop == null) {
        SLLogger.getLogger().log(Level.WARNING, "Got null drop", new Throwable());
        continue;
      }
      // only count promises within the source code
      if (drop instanceof PromiseDrop && !((PromiseDrop) drop).isFromSrc()) {
        continue;
      }
      String key = computeKey(drop);
      updateCountMap(droptypeToCount, key, 1);

      if (drop instanceof BinaryCUDrop) {
        updateCountMap(droptypeToCount, "bLOC", ((BinaryCUDrop) drop).lines);
      }
      if (drop instanceof SourceCUDrop) {
        updateCountMap(droptypeToCount, "sLOC", ((SourceCUDrop) drop).lines);
      }
    }
    return droptypeToCount;
  }

  public static Set<Drop> getDropsAssociatedWithModel(ModelDrop model) {
    /*
     * if (model.hasDeponents()) { // Not an independent model (probably a
     * RegionModel) return Collections.EMPTY_SET; }
     */
    final Set<Drop> drops = new HashSet<Drop>();
    final DropPredicate pred = new DropPredicate() {
      /*
       * Used to catch every drop that gets added, so we can get its dependents
       */
      public boolean match(IDropInfo d) {
        if (!(d instanceof Drop)) {
          return false;
        }

        final Drop drop = (Drop) d;
        if (drops.contains(drop) || drop instanceof ModelDrop) {
          // the model drop will be handled separately
          return false;
        }

        drops.addAll(drop.getMatchingDependents(this));

        // Add the corresponding comp unit drops
        if (drop instanceof IRReferenceDrop) {
          final IRReferenceDrop ird = (IRReferenceDrop) drop;
          IRNode cu = VisitUtil.getEnclosingCompilationUnit(ird.getNode());
          CUDrop cud = CUDrop.queryCU(cu);
          drops.add(cud);
        }
        return true;
      }
    };
    // adds recursively with the predicate
    drops.addAll(model.getMatchingDependents(pred));
    return drops;
  }

  public static Set<Drop> getAssociatedDrops(final Drop startDrop) {
    final Set<Drop> drops = new HashSet<Drop>();
    return getAssociatedDrops(drops, startDrop);
  }

  public static Set<Drop> getAssociatedDrops(final Set<Drop> drops, final Drop startDrop) {
    final DropPredicate pred = new DropPredicate() {
      /*
       * Used to catch every drop that gets added, so we can get its dependents
       */
      public boolean match(IDropInfo d) {
        if (!(d instanceof Drop)) {
          return false;
        }

        final Drop drop = (Drop) d;
        if (drops.contains(drop)) {
          return false;
        }
        drops.addAll(drop.getMatchingDependents(this));
        return true;
      }
    };
    // adds recursively with the predicate
    drops.addAll(startDrop.getMatchingDependents(pred));
    return drops;
  }

  public static Map generateAssociatedDropCounts(ModelDrop model) {
    Set<Drop> drops = getDropsAssociatedWithModel(model);
    if (drops.isEmpty()) {
      return Collections.EMPTY_MAP;
    }
    return generateDropCounts(drops);
  }

  public static List<String> interpretDropCounts(Map<String, Integer> droptypeToCount) {
    if (droptypeToCount.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> out = new ArrayList<String>();
    for (Iterator<String> j = droptypeToCount.keySet().iterator(); j.hasNext();) {
      String key = j.next();
      int count = droptypeToCount.get(key).intValue();
      String cs = " " + count;
      String pad = "       ";
      cs = (cs.length() < pad.length() ? pad.substring(cs.length()) + cs : cs);
      out.add(cs + " " + key);
    }
    return out;
  }

  public static void printDropCounts(PrintWriter out, List<String> counts, String title) {
    out.println("DROP METRICS for " + title);
    out.println("------------");

    for (Iterator<String> j = counts.iterator(); j.hasNext();) {
      String count = j.next();
      out.println(count);
    }
    out.println("------------");
    out.flush();
  }

  /**
   * Generates a crude drop-sea report to a file
   */
  public static void generateReport(PrintWriter out) {
    out.println("=============================");
    out.println("FLUID ASSURANCE STATUS REPORT");
    out.println("=============================");
    out.println();

    Date current = new Date();
    DateFormat currentFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    String currentDateAndTime = currentFormat.format(current);

    out.println(currentDateAndTime);
    out.println();

    long plusResults = 0;
    long redXResults = 0;
    Set<? extends ResultDrop> results = Sea.getDefault().getDropsOfType(ResultDrop.class);
    for (ResultDrop result : results) {
      // ResultDrop result = i.next();
      line(out);

      // RESULT //

      out.println((result.isConsistent() ? "\"+\"  " : "\"X\"  ") + result.getMessage());
      ISrcRef sr = result.getSrcRef();
      if (sr != null) {
        Object file = sr.getEnclosingFile();
        if (file != null) {
          out.println("          in " + sr.getEnclosingFile() + " [line " + sr.getLineNumber() + "]");
        }
      }

      // just counters
      if (result.isConsistent()) {
        plusResults++;
      } else {
        redXResults++;
      }

      // CHECKS //

      boolean first = true;
      for (PromiseDrop promise : result.getChecks()) {
        if (first) {
          first = false;
          out.println("     Promises this result CHECKS (partially establishes):");
        }
        out.println("     | " + promise.getMessage());
        sr = promise.getSrcRef();
        if (sr != null) {
          if (sr.getEnclosingFile() != null) {
            out.println("     |      in " + sr.getEnclosingFile() + " [line " + sr.getLineNumber() + "]");
          }
        }
      }

      // TRUSTS (preconditions) //

      first = true;
      for (PromiseDrop promise : result.getTrusts()) {
        if (first) {
          first = false;
          out.println("     Promises this result TRUSTS (requires as preconditions):");
        }
        out.println("     | " + promise.getMessage());
        sr = promise.getSrcRef();
        if (sr != null) {
          Object f = sr.getEnclosingFile();
          if (f instanceof String) {
            out.println("     |      in " + f);
          } else if (f != null) {
            out.println("     |      in " + f + " [line " + sr.getLineNumber() + "]");
          }
        }

      }

      line(out);
    }
    out.println();
    String cs = " " + plusResults;
    String pad = "       ";
    cs = (cs.length() < pad.length() ? pad.substring(cs.length()) + cs : cs);
    out.println(cs + " \"+\" (positive) program analysis results");
    cs = " " + redXResults;
    pad = "       ";
    cs = (cs.length() < pad.length() ? pad.substring(cs.length()) + cs : cs);
    out.println(cs + " \"X\" (negative) program analysis results");
    out.println();
    out.println("END OF REPORT");

    out.close();
  }

  private static void line(PrintWriter p) {
    p.println("-------------------------------------------------------------");
  }
}
