package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

public abstract class DropMatcher {
	private final String[] labels;

	protected DropMatcher(String... labels) {
		if (labels == null) {
			throw new IllegalArgumentException("Null labels");
		}
		for(String l : labels) {
			if (l == null) {
				throw new IllegalArgumentException("Null label");
			}
			if (l.length() != 7) {
				throw new IllegalArgumentException("Incorrect length: "+l);
			}
		}
		this.labels = labels;
	}

	final int numPasses() {
		return labels.length;
	}

	/**
	 * @return A String of length 7
	 */
	final String getLabel(int pass) {
		return labels[pass];
	}

	protected abstract boolean warnIfMatched(int pass);
	
	protected abstract boolean match(int pass, IDrop n, IDrop o);

	protected static boolean matchBasics(IDrop n, IDrop o) {
		return /*matchCategory(n, o) && */matchMessage(n, o) && matchProvedConsistent(n, o);
		//&& matchSupportingInfo(n, o);
	}
	
	protected static boolean matchCategory(IDrop n, IDrop o) {
		String nCat = n.getCategorizingMessage();
		String oCat = o.getCategorizingMessage();
		if (nCat != null && oCat != null) {
			return nCat.equals(oCat);
		}
		// If either null, don't compare
		return true;
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
	
	private static String preprocess(String value) {
		if (value == null) {
			return null;
		}
		return value.trim();
	}
	
	protected static Boolean matchStrings(String n, String o) {
		String nMsg = preprocess(n);
		String oMsg = preprocess(o);
		if (nMsg != null && oMsg != null) {
			return nMsg.equals(oMsg);
		}
		return null;
	}
	
	protected static boolean matchMessage(IDrop n, IDrop o) {		
		Boolean result;
		if (!o.getMessageCanonical().endsWith(" (EMPTY)")) { // TODO only needed for summaries
			result = matchStrings(n.getMessageCanonical(), o.getMessageCanonical());
			if (result != null) {
				return result;
			}
		}
		result = matchStrings(n.getMessage(), o.getMessage());
		return result != null ? result : false;
	}
	
	protected static boolean matchSupportingInfo(IDrop n, IDrop o) {
		final long oh = computeSIHash(o);
		if (DropDiff.allowMissingSupportingInfos && oh == 0) {
			return true;
		}
		final long nh = computeSIHash(n);
		if (nh != oh) {
			//System.out.println("Diff in infos");
			return false;
		}
		return DropDiff.isSame(n, o);
	}

	private static long computeSIHash(IDrop e) {
		long rv = 0;  
		for (IHintDrop i : e.getHints()) {
			rv += i.getHintType().hashCode();
			
			String msg = i.getMessage();
			if (msg == null) {
				continue;
			}
			rv += msg.hashCode();
		}
		return rv;
	}
	
	protected static boolean matchLong(Long n, Long o) {
		if (n != null && o != null) {
			return n.equals(o);
		}
		return false;
	}
	
	protected static Long getOffset(IDrop d) {
		ISrcRef ref = d.getSrcRef();
		if (ref != null) {
			return (long) ref.getOffset();
		}
		return null;
	}
}
