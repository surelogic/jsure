package com.surelogic.dropsea.irfree;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.surelogic.common.IViewable;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IDrop;

public class DropDiff extends DiffNode implements IViewable {
	static boolean allowMissingSupportingInfos = false;
	
	final IDrop old;
	final Object[] children;

	private DropDiff(IDrop n, IDrop o, Object[] children) {
		super(n);
		this.children = children;
		this.old = o;
	}

	@Override
	public String toString() {
		return drop.getMessage();
	}

	public String getText() {
		return toString();
	}

	public boolean hasChildren() {
		// This exists because there are diffs
		return true;
	}

	public Object[] getChildren() {
		return children;
	}

	static DropDiff compute(PrintStream out, DiffNode n, DiffNode o) {
		if (o.drop.getHints().isEmpty()) {
			if (n.drop.getHints().isEmpty()) {
				return null;
			}
			if (allowMissingSupportingInfos) {
				// System.out.println("Temporarily ignoring missing details in old oracles");
				return null;
			}
		}
		final Map<String, DiffNode> oldDetails = extractDetails(o.drop);
		final Map<String, DiffNode> newDetails = extractDetails(n.drop);
		final List<String> temp = new ArrayList<String>();
		// Remove matching ones
		for (String ns : newDetails.keySet()) {
			DiffNode oe = oldDetails.remove(ns);
			if (oe != null) {
				temp.add(ns);
			}
		}
		for (String match : temp) {
			newDetails.remove(match);
		}

		if (oldDetails.isEmpty() && newDetails.isEmpty()) {
			return null;
		}
		out.println("\tDiffs in details for " + n.drop.getMessage());
		for (String old : sort(oldDetails.keySet(), temp)) {
			out.println("\t\tOld    : " + old);
			DiffNode e = oldDetails.get(old);
			e.setAsOld();
		}
		for (String newMsg : sort(newDetails.keySet(), temp)) {
			out.println("\t\tNewer  : " + newMsg);
			DiffNode e = newDetails.get(newMsg);
			e.setAsNewer();
		}
		List<DiffNode> remaining = new ArrayList<DiffNode>(oldDetails.size() + newDetails.size());
		remaining.addAll(oldDetails.values());
		remaining.addAll(newDetails.values());
		Collections.sort(remaining);
		return new DropDiff(n.drop, o.drop, remaining.toArray());
	}
	
	// Assume that we only have supporting info
	public static Map<String, DiffNode> extractDetails(IDrop e) {
		if (e.getHints().isEmpty()) {
			return Collections.emptyMap();
		}
		final Map<String, DiffNode> rv = new TreeMap<String, DiffNode>();
		for (IHintDrop i : e.getHints()) {
			String msg = i.getHintType()+" : "+i.getMessage();
			if (msg != null) {
				rv.put(msg, new DiffNode(i));
			} else {
				System.out.println("No message for " + i);
			}
		}
		return rv;
	}
	
	private static Collection<String> sort(Collection<String> s, List<String> temp) {
	    temp.clear();
	    temp.addAll(s);
	    Collections.sort(temp);
	    return temp;
	}

	public void write(PrintWriter w) {
		w.println("\tDiffs in details for " + drop.getMessage());
		final Map<String, DiffNode> oldDetails = extractDetails(old);
		final Map<String, DiffNode> newDetails = extractDetails(drop);
		final List<String> temp = new ArrayList<String>();
		for (String old : sort(oldDetails.keySet(), temp)) {
			w.println("\t\tOld    : " + old);
		}
		for (String newMsg : sort(newDetails.keySet(), temp)) {
			w.println("\t\tNewer  : " + newMsg);
		}		
	}

	public static boolean isSame(IDrop n, IDrop o) {
		if (n.getHints().isEmpty() && o.getHints().isEmpty()) {
			return true;
		}
		if (n.getHints().size() != o.getHints().size()) {
			return false;
		}
		final Map<String, DiffNode> oldDetails = extractDetails(o);
		final Map<String, DiffNode> newDetails = extractDetails(n);
		// Remove matching ones
		for (String ns : newDetails.keySet()) {
			DiffNode oe = oldDetails.remove(ns);
			if (oe == null) {
				// New not in the old
				return false;
			}
		}
		return oldDetails.isEmpty();
	}
}
