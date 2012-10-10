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
import com.surelogic.dropsea.IProofDrop;

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
				return diffProperties(out, n, o);
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
			return diffProperties(out, n, o);
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
		List<AbstractDiffNode> remaining = new ArrayList<AbstractDiffNode>(1 + oldDetails.size() + newDetails.size());
		if (!matchProvedConsistent(n.drop, o.drop)) {
			DiffMessage m = makeProvedConsistentMsg(out, n.drop, o.drop);	
			remaining.add(m);
		}
		remaining.addAll(oldDetails.values());
		remaining.addAll(newDetails.values());
		Collections.sort(remaining);
		return new DropDiff(n.drop, o.drop, remaining.toArray());
	}

	private static DropDiff diffProperties(PrintStream out, DiffNode n, DiffNode o) {
		if (matchProvedConsistent(n.drop, o.drop)) {
			return null;
		}
		out.println("\tDiffs in details for " + n.drop.getMessage());
		DiffMessage m = makeProvedConsistentMsg(out, n.drop, o.drop);		
		return new DropDiff(n.drop, o.drop, wrap(m));
	}
	
	private static DiffMessage[] wrap(DiffMessage s) {
		return new DiffMessage[] { s };
	}
	
	private static DiffMessage makeProvedConsistentMsg(PrintStream out, IDrop n, IDrop o) {
		String msg = "provedConsistent: "+provedConsistent(o)+" => "+provedConsistent(n);
		out.println("\t\tChanged: "+msg);
		return new DiffMessage(msg, Status.CHANGED);
	}
	
	private static boolean provedConsistent(IDrop d) {
		if (d instanceof IProofDrop) {
			IProofDrop pd = (IProofDrop) d;
			return pd.provedConsistent();
		}
		return false;
	}
	
	protected static boolean matchProvedConsistent(IDrop n, IDrop o) {
		return provedConsistent(n) == provedConsistent(o);
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
		for(Object o : getChildren()) {
			AbstractDiffNode n = (AbstractDiffNode) o;
			switch (n.getDiffStatus()) {
			case CHANGED:
				w.println("\t\tChanged: " + n.getText());
				break;
			case NEW:
				w.println("\t\tNewer  : " + n.getText());
				break;
			case OLD:
				w.println("\t\tOld    : " + n.getText());
				break;
			}
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
