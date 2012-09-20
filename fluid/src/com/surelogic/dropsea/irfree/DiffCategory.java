package com.surelogic.dropsea.irfree;

import java.io.*;
import java.util.*;

import com.surelogic.common.*;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

public final class DiffCategory<K extends Comparable<K>> implements IViewable, Comparable<DiffCategory<K>> {
	final K key;
	final Set<DiffNode> old = new HashSet<DiffNode>();
	final Set<DiffNode> newer = new HashSet<DiffNode>();
	final Set<DropDiff> diffs = new HashSet<DropDiff>();

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
		for(IDrop d : o) {
			addOld(d);
		}
	}

	public void addAllNew(Collection<? extends IDrop> n) {
		for(IDrop d : n) {
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
	
	public void diff(PrintStream out, DropMatcher m) {
		String title = "Category: " + getText();
		for(int i=0; i<m.numPasses(); i++) {
			title = match(title, out, m, i);
		}
		/*
		title = match(title, out, EXACT, "Exact  ");
		title = match(title, out, HASHED, "Hashed ");
		title = match(title, out, HASHED2, "Hashed2");
		// title = match(title, out, SAME_LINE, "Line   ");
		if (name.endsWith(".ResultDrop")) {
			title = match(title, out, RESULT, "Results");
		}
		*/
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

	private String match(String title, PrintStream out, DropMatcher m, final int pass) {
		Iterator<DiffNode> it = newer.iterator();
		while (it.hasNext()) {
			DiffNode n = it.next();
			for (DiffNode o : old) {
				if (m.match(pass, n.drop, o.drop)) {
					String label = m.getLabel(pass);
					if (!"Exact  ".equals(label) && !"Hashed ".equals(label)) {
						if (title != null) {
							out.println(title);
							title = null;
						}
						out.println("\t" + label + ": " + toString(n));
					}
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
	          Long l = DropMatcher.getOffset(d.drop);
	          if (l == null) {
	        	  return -1;
	          }
	          return l.intValue();
	        }
	      });
	      return l;
	}
	
	static int getOffset(ISrcRef ref) {
		if (ref == null) {
			return -1;
		}
		return ref.getOffset();
	}
	
	private static String toString(DiffNode n) {
		IDrop d = n.drop;
		ISrcRef ref = d.getSrcRef();
		boolean proved = false;
		if (d instanceof IProofDrop) {
			IProofDrop p = (IProofDrop) d;
			proved = p.provedConsistent();
		}
		// FIX to use accessors
		if (ref == null) {
			return "null - null - null - "+ proved + " - " + d.getMessageCanonical() + " - " + d.getMessage();
		} else {
		      return ref.getOffset() + " - " + d.getTreeHash() + " - " + d.getContextHash() + " - "
	          + proved + " - " + d.getMessageCanonical() + " - " + d.getMessage();
		}
	}
}
