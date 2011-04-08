package edu.cmu.cs.fluid.sea;

import java.util.*;

public final class SeaStats {
	private SeaStats() {
		// Nothing to do
	}
	
	public interface Splitter<T> {
		T getLabel(IDropInfo d);
	}
	
	public static final <T> Map<T,Set<IDropInfo>> split(Collection<? extends IDropInfo> d, Splitter<T> split) {
		final Map<T,Set<IDropInfo>> rv = new HashMap<T, Set<IDropInfo>>();
		for(IDropInfo i : d) {
			final T label = split.getLabel(i);
			Set<IDropInfo> labelled = rv.get(label);
			if (labelled == null) {
				labelled = new HashSet<IDropInfo>();
				rv.put(label, labelled);
			}
			labelled.add(i);
		}
		return rv;
	}
	
	public interface Counter {
		String count(IDropInfo d);
	}
	
	public static final Map<String,Integer> count(Collection<? extends IDropInfo> d, Counter[] counters) {
		Map<String,Integer> counts = new HashMap<String, Integer>();
		for(IDropInfo i : d) {
			for(Counter c : counters) {
				String label = c.count(i);
				if (label != null) {
					Integer count = counts.get(label);
					if (count == null) {
						counts.put(label, 1);
					} else {
						counts.put(label, count+1);
					}
				}
			}
		}
		return counts;
	}
	
	public static final <T> Map<T,Map<String,Integer>> count(Map<T,Set<IDropInfo>> info, Counter[] counters) {
		Map<T,Map<String,Integer>> rv = new HashMap<T, Map<String,Integer>>();
		for(Map.Entry<T,Set<IDropInfo>> e : info.entrySet()) {
			rv.put(e.getKey(), count(e.getValue(), counters));
		}
		return rv;
	}
	
	public static final String PROMISES = "Promises";
	public static final String CONSISTENT = "Consistent";
	public static final String INCONSISTENT = "Inconsistent";
	public static final String VOUCHES = "Vouches";
	public static final String ASSUMES = "Assumes";
	/* What else is interesting?
	public static final String CONSISTENT = "Consistent";
	public static final String CONSISTENT = "Consistent";
	public static final String CONSISTENT = "Consistent";
	public static final String CONSISTENT = "Consistent";
	*/
	public static final Counter[] STANDARD_COUNTERS = {
		
	};
}
