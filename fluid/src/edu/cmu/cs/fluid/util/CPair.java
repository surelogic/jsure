package edu.cmu.cs.fluid.util;

import com.surelogic.common.Pair;

/**
 * Comparable Pair
 */
public final class CPair<T1 extends Comparable<T1>, T2 extends Comparable<T2>> 
extends Pair<T1,T2> implements Comparable<CPair<T1,T2>>{
	public CPair(T1 o1, T2 o2) {
		super(o1, o2);
	}

//	@Override
	public int compareTo(CPair<T1, T2> o) {
		int rv = first().compareTo(o.first());
		if (rv == 0) {
			rv = second().compareTo(o.second());
		}
		return rv;
	}
}
