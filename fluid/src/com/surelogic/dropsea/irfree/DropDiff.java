package com.surelogic.dropsea.irfree;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

import com.surelogic.common.IViewable;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;

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

	@Override
  public String getText() {
		return toString();
	}

	@Override
  public boolean hasChildren() {
		// This exists because there are diffs
		return true;
	}

	@Override
  public Object[] getChildren() {
		return children;
	}

	/**
	 * Print out the title (if non-null) and the diffs
	 * @param label 
	 */
	static DropDiff compute(String title, PrintStream out, String label, DiffNode n, DiffNode o) {
		//if (o.drop.getHints().isEmpty()) {
		//	if (n.drop.getHints().isEmpty()) {
				DiffMessage m = diffProperties(title, out, label, n, o);
				if (m != null) {
					return new DropDiff(n.drop, o.drop, wrap(m));
				} else {
					return null;
				}
		//	}
		//}
		/*
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
			DiffMessage m = diffProperties(title, out, label, n, o);
			if (m != null) {
				return new DropDiff(n.drop, o.drop, wrap(m));
			} else {
				return null;
			}
		}
		if (title != null) {
			out.println(title);
		}
		out.println("\tDiffs in details for: " + DiffCategory.toString(n));
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
		DiffMessage m = diffProperties(null, out, label, n, o);	
		if (m != null) {
			remaining.add(m);
		}
		remaining.addAll(oldDetails.values());
		remaining.addAll(newDetails.values());
		Collections.sort(remaining);
		return new DropDiff(n.drop, o.drop, remaining.toArray());
		*/
	}

	/**
	 * Print out the title (if non-null) and the diffs
	 * @param label 
	 */
	private static DiffMessage diffProperties(String title, PrintStream out, String label, DiffNode n, DiffNode o) {
		if (n.drop instanceof IProofDrop) {
			final IProofDrop pn = (IProofDrop) n.drop;
			final IProofDrop po = (IProofDrop) o.drop;
			for(ProofPredicate p : predicates) {
				final String msg = p.match(pn, po);
				if (msg != null) {
					if (title != null) {
						out.println(title);
					}
					out.println("\t"+label+" Diffs in details for: " + DiffCategory.toString(n));
					out.println(msg);
					final String nh = n.drop.getDiffInfoOrNull(DiffHeuristics.ANALYSIS_DIFF_HINT);
					if (nh != null) {
						out.println("\t\tNew hint: "+nh);
					}
					final String oh = o.drop.getDiffInfoOrNull(DiffHeuristics.ANALYSIS_DIFF_HINT);
					if (oh != null) {
						out.println("\t\tOld hint: "+oh);
					}
					return new DiffMessage(msg, Status.CHANGED) {
						@Override
						public IDrop getDrop() {
							return pn;
						}
					};
				}
			}
		}
		return null;
	}
	
	private static DiffMessage[] wrap(DiffMessage s) {
		return new DiffMessage[] { s };
	}
	
	private static abstract class ProofPredicate {
		private final String label;

		ProofPredicate(String l) {
			label = l;
		}
		/**
		 * @return non-null if different
		 */
		String match(final IProofDrop n, final IProofDrop o) {
			if (getAttr(n) != getAttr(o)) {
				return "\t\tChanged: "+label+": "+getAttr(o)+" => "+getAttr(n);
			}
			return null;
		}
		
		abstract boolean getAttr(IProofDrop d);
	}
	private static abstract class ResultPredicate extends ProofPredicate {
		ResultPredicate(String l) {
			super(l);
		}
		@Override
    final boolean getAttr(IProofDrop d) {
			if (d instanceof IResultDrop) {
				final IResultDrop rd = (IResultDrop) d;
				return getAttr(rd);
			}
			return false;
		}
		abstract boolean getAttr(IResultDrop d);
	}
	
	private static final ProofPredicate[] predicates = {
		new ProofPredicate("provedConsistent") {
			@Override
			public boolean getAttr(IProofDrop d) {
				return d.provedConsistent();
			}
		},
		new ProofPredicate("proofUsesRedDot") {
			@Override
			public boolean getAttr(IProofDrop d) {
				return d.proofUsesRedDot();
			}
		},
		new ResultPredicate("isConsistent") {
			@Override
			public boolean getAttr(IResultDrop rd) {
				return rd.isConsistent();
			}
		}, 
		new ResultPredicate("isTimeout") {
			@Override
			public boolean getAttr(IResultDrop rd) {
				return rd.isTimeout();
			}
		},
		new ResultPredicate("isVouched") {
			@Override
			public boolean getAttr(IResultDrop rd) {
				return rd.isVouched();
			}
		},
	};
	
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
	/*
	private static Collection<String> sort(Collection<String> s, List<String> temp) {
	    temp.clear();
	    temp.addAll(s);
	    Collections.sort(temp);
	    return temp;
	}
	*/

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
			default:
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
