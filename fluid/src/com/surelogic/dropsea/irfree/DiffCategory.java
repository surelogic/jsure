package com.surelogic.dropsea.irfree;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.common.IViewable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.*;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;

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

  @Override
  public boolean hasChildren() {
    return !old.isEmpty() || !newer.isEmpty() || !diffs.isEmpty();
  }

  @Override
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
      @Override
      public int compare(Object o1, Object o2) {
        return o1.toString().compareTo(o2.toString());
      }
    });
    for (DiffNode e : a) {
      temp.add(e);
    }
    return temp.toArray();
  }

  @Override
  public String getText() {
    return key.toString(); // FIX
  }

  @Override
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

    /*
    // Null if the title's already printed
    if (title != null && hasChildren()) {
      out.println(title);
    }
    */
    /*
     * for(Entity o : sortByOffset(old)) {
     * out.println("\tOld    : "+toString(o)); } for(Entity o :
     * sortByOffset(newer)) { out.println("\tNewer  : "+toString(o)); }
     */
    for (DiffNode o : sortByOffset(old)) {
      final String msg = toString(o);
      if (suppressOld(o.drop)) {  
    	  old.remove(o);
      } else {
    	  title = printTitle(out, title);
    	  out.println("\tOld    : " + msg);
      }
    }
    for (DiffNode o : sortByOffset(newer)) {
   	  if (suppress(o.drop)) {  
   		  newer.remove(o);
   	  } else {
    	  title = printTitle(out, title);
   		  out.println("\tNewer  : " + toString(o));
   	  }
    }
  }

  private String printTitle(final PrintStream out, String title) {
	  if (title != null) {
		  out.println(title);
	  }
	  return null;
  }
  
  /*
  private String[] resultFilterPrefixes = {
		  "Borrowed parameters of",
		  "Effects of",
		  "Unique return value of"
  };
  */
  
  private boolean suppressOld(IDrop o) {
	  if (o instanceof IProposedPromiseDrop &&
	      o.getMessage().startsWith("(proposed promise)  @RegionEffects")) {
	      IProposedPromiseDrop p = (IProposedPromiseDrop) o;
	      if (p.getOrigin() == Origin.CODE) {
	    	  return true;
	      }
	  }
	  else if (o instanceof IHintDrop && 
		  o.getMessage().contains("<clinit> has effect")) {
		  return true;
	  }
	  /*
	  if (o instanceof IResultDrop) {
		  for(String prefix : resultFilterPrefixes) {
			  if (o.getMessage().startsWith(prefix)) {
				  return true;
			  }
		  }
	  }
	  if (o instanceof IResultFolderDrop) {
		  return o.getMessage().startsWith("Parameterized type");
	  }
	  return false;
	  */
	  return suppress(o);
  }
  
  private boolean suppress(IDrop o) {
	  return isNotDerivedFromSrc(o);
  }  
  
  /**
   * Print out the title (if non-null) and the diffs
   */
  private String match(String title, PrintStream out, IDropMatcher m) {
	if (m.useHashing()) {
		return matchUsingHashes(title, out, m);
	}
    Iterator<DiffNode> it = newer.iterator();
    while (it.hasNext()) {
      DiffNode n = it.next();
      for (DiffNode o : old) {
        if (m.match(n.drop, o.drop)) {
          final String label = m.getLabel();
          if (m.warnIfMatched()) {
            if (title != null) {
              out.println(title);
              title = null;
            }
            out.println("\t" + label + ":+" + toString(n));
            out.println("\t" + label + ":-" + toString(o));
          }

          old.remove(o);
          it.remove();
          if (isNotDerivedFromSrc(n.drop) && isNotDerivedFromSrc(o.drop)) {
        	  // No need to diff the match, since we can ignore these
        	  break;
          }
          title = diffMatchingDrops(title, out, label, n, o);
          break;
        }
      }
    }
    return title;
  }
  
  private String diffMatchingDrops(String title, PrintStream out, String label, DiffNode n, DiffNode o) {
      DropDiff d = DropDiff.compute(title, out, label, n, o);
      if (d != null) {
    	title = null;
        diffs.add(d);
      } else {
    	newMatchingOld.put(n.drop, o.drop);
      }
      return title;
  }
  
  private String matchUsingHashes(String title, PrintStream out, IDropMatcher m) {	  
	  final long start = System.currentTimeMillis();
	  // Cache hashs 
	  final MultiMap<Integer, DiffNode> older = new MultiHashMap<Integer, DiffNode>();
	  for(DiffNode o : old) {
		  o.cachedHash = m.hash(o.drop);
		  older.put(o.cachedHash, o);
	  }	
	  
	  Iterator<DiffNode> it = newer.iterator();
	  while (it.hasNext()) {
		  final DiffNode n = it.next();
		  n.cachedHash = m.hash(n.drop);
		  
		  Collection<DiffNode> hashedOld = older.get(n.cachedHash);
		  if (hashedOld == null) {
			  continue;
		  }
		  for (final DiffNode o : hashedOld) {
			  if (m.match(n.drop, o.drop)) {
				  final String label = m.getLabel();
				  if (m.warnIfMatched()) {
					  if (title != null) {
						  out.println(title);
						  title = null;
					  }
					  out.println("\t" + label + ":+" + toString(n));
					  out.println("\t" + label + ":-" + toString(o));
				  }

				  hashedOld.remove(o);
				  old.remove(o);
				  it.remove();
				  if (isNotDerivedFromSrc(n.drop) && isNotDerivedFromSrc(o.drop)) {
					  // No need to diff the match, since we can ignore these
					  break;
				  }
		          title = diffMatchingDrops(title, out, label, n, o);
				  break;
			  }
		  }
	  }
	  final long end = System.currentTimeMillis();
	  final long time = end - start;
	  if (time > 100) {
		  System.out.println(getText()+" took "+time+" ms");
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
      @Override
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
  static String toString(DiffNode n) {
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
      		 d.getDiffInfoAsInt(DiffHeuristics.DECL_RELATIVE_OFFSET, -1) + " - " +
      		 d.getDiffInfoAsInt(DiffHeuristics.DECL_END_RELATIVE_OFFSET, -1) + " - " +
      		 d.getDiffInfoAsLong(DiffHeuristics.FAST_TREE_HASH, -1) + " - " +
       		 d.getDiffInfoAsLong(DiffHeuristics.FAST_CONTEXT_HASH, -1) + " - " + ref;
    }
  }
  
  static boolean isNotDerivedFromSrc(IDrop d) {
	  if (d instanceof IProofDrop) {
	      IProofDrop pd = (IProofDrop) d;
	      return !pd.derivedFromSrc();
	  }
	  return false;
  }
}
