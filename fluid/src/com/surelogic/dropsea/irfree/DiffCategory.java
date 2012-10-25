package com.surelogic.dropsea.irfree;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

import com.surelogic.common.IViewable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDiffInfo;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;
import static com.surelogic.dropsea.IDiffInfo.*;

public final class DiffCategory<K extends Comparable<K>> implements IViewable, Comparable<DiffCategory<K>> {
  final K key;
  final Set<DiffNode> old = new HashSet<DiffNode>();
  final Set<DiffNode> newer = new HashSet<DiffNode>();
  final Set<DropDiff> diffs = new HashSet<DropDiff>();
  final Map<IDrop,IDrop> newMatchingOld = new HashMap<IDrop, IDrop>();

  DiffCategory(K key) {
    this.key = key;
  }

  public void addOld(IDrop d) {
    old.add(new DiffNode(d));
  }

  public void addNew(IDrop d) {
    newer.add(new DiffNode(d));
  }

  public void addAllOld(Collection<? extends IDrop> o) {
    for (IDrop d : o) {
      addOld(d);
    }
  }

  public void addAllNew(Collection<? extends IDrop> n) {
    for (IDrop d : n) {
      addNew(d);
    }
  }

  public boolean isEmpty() {
    return !hasChildren();
  }

  public boolean hasChildren() {
    return !old.isEmpty() || !newer.isEmpty() || !diffs.isEmpty();
  }

  public Object[] getChildren() {
    DiffNode[] a = new DiffNode[old.size() + newer.size()];
    int i = 0;
    for (DiffNode o : old) {
      a[i] = o;
      o.setAsOld();
      i++;
    }
    for (DiffNode o : newer) {
      a[i] = o;
      o.setAsNewer();
      i++;
    }
    Arrays.sort(a);
    if (diffs.isEmpty()) {
      return a;
    }
    List<Object> temp = new ArrayList<Object>(diffs.size() + a.length);
    temp.addAll(diffs);
    Collections.sort(temp, new Comparator<Object>() {
      public int compare(Object o1, Object o2) {
        return o1.toString().compareTo(o2.toString());
      }
    });
    for (DiffNode e : a) {
      temp.add(e);
    }
    return temp.toArray();
  }

  public String getText() {
    return key.toString(); // FIX
  }

  public int compareTo(DiffCategory<K> other) {
    return key.compareTo(other.key);
  }

  public void diff(final PrintStream out, final CategoryMatcher m) {
    String title = "Category: " + getText();
    for (int i = 0; i < m.numPasses(); i++) {
      final IDropMatcher pass = m.getPass(i);
      title = match(title, out, pass);
    }
    if (isEmpty()) {
      return;
    }

    if (title != null && hasChildren()) {
      out.println(title);
    }
    /*
     * for(Entity o : sortByOffset(old)) {
     * out.println("\tOld    : "+toString(o)); } for(Entity o :
     * sortByOffset(newer)) { out.println("\tNewer  : "+toString(o)); }
     */
    for (DiffNode o : sortByOffset(old)) {
      final String msg = toString(o);
      out.println("\tOld    : " + msg);
    }
    for (DiffNode o : sortByOffset(newer)) {
      out.println("\tNewer  : " + toString(o));
    }
  }

  private String match(String title, PrintStream out, IDropMatcher m) {
    Iterator<DiffNode> it = newer.iterator();
    while (it.hasNext()) {
      DiffNode n = it.next();
      for (DiffNode o : old) {
        if (m.match(n.drop, o.drop)) {
          String label = m.getLabel();
          if (m.warnIfMatched()) {
            if (title != null) {
              out.println(title);
              title = null;
            }
            out.println("\t" + label + ":+" + toString(n));
            out.println("\t" + label + ":-" + toString(o));
          }
          newMatchingOld.put(n.drop, o.drop);
          old.remove(o);
          it.remove();
          DropDiff d = DropDiff.compute(out, n, o);
          if (d != null) {
            diffs.add(d);
          }
          break;
        }
      }
    }
    return title;
  }

  public void write(PrintWriter w) {
    if (!isEmpty()) {
      w.println("Category: " + getText());
      if (!diffs.isEmpty()) {
        for (DropDiff d : diffs) {
          d.write(w);
        }
      }
      for (DiffNode o : old) {
        w.println("\tOld    : " + toString(o));
      }
      for (DiffNode o : newer) {
        w.println("\tNewer  : " + toString(o));
      }
      w.println();
    }
  }

  private static List<DiffNode> sortByOffset(Collection<DiffNode> unsorted) {
    List<DiffNode> l = new ArrayList<DiffNode>(unsorted);
    Collections.sort(l, new Comparator<DiffNode>() {
      public int compare(DiffNode o1, DiffNode o2) {
        return offset(o1) - offset(o2);
      }

      private int offset(DiffNode d) {
        return CategoryMatcher.getOffset(d.drop);
      }
    });
    return l;
  }

  /*
   * static int getOffset(ISrcRef ref) { if (ref == null) { return -1; } return
   * ref.getOffset(); }
   */
  private static String toString(DiffNode n) {
    IDrop d = n.drop;
    IJavaRef ref = d.getJavaRef();
    boolean proved = false;
    if (d instanceof IProofDrop) {
      IProofDrop p = (IProofDrop) d;
      proved = p.provedConsistent();
    }
    // FIX to use accessors
    if (ref == null) {
      return proved + " - " + d.getMessageCanonical() + " - " + d.getMessage();
    } else {
      return proved + " - " + d.getMessageCanonical() + " - " + d.getMessage() + " - " +
      		 d.getDiffInfoAsInt(DECL_RELATIVE_OFFSET, -1) + " - " +
      		 d.getDiffInfoAsInt(DECL_END_RELATIVE_OFFSET, -1) + " - " +
      		 d.getDiffInfoAsLong(FAST_TREE_HASH, -1) + " - " +
       		 d.getDiffInfoAsLong(FAST_CONTEXT_HASH, -1) + " - " + ref;
    }
  }
}
