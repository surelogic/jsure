package edu.cmu.cs.fluid.sea;

import java.util.HashMap;
import java.util.Map;

public final class DropPredicateFactory {

	private static Map<Class, DropPredicate> f_type = new HashMap<Class, DropPredicate>();

	/**
	 * Returns a drop predicate that matches all drops that are instances of the
	 * given class and its subclasses. This factory should be used rather than
	 * constructing new drop predicates for this purpose because a cache is
	 * maintained.
	 * 
	 * @param dropClass
	 *            the class to match against (should be a class that is a
	 *            subclass of {@link Drop}).
	 * @return the drop predicate.
	 */
	public static DropPredicate matchType(final Class dropClass) {
		DropPredicate result = f_type.get(dropClass);
		if (result == null) {
			result = new DropPredicate() {
				public boolean match(Drop d) {
					return dropClass.isInstance(d);
				}
			};
			f_type.put(dropClass, result);
		}
		return result;
	}

	private static Map<Class, DropPredicate> f_exactType = new HashMap<Class, DropPredicate>();

	/**
	 * Returns a drop predicate that matches all drops that are instances of the
	 * given class, not including subclasses. This factory should be used rather
	 * than constructing new drop predicates for this purpose because a cache is
	 * maintained.
	 * 
	 * @param dropClass
	 *            the class to match against (should be a class that is subclass
	 *            of {@link Drop}).
	 * @return the drop predicate.
	 */
	public static DropPredicate matchExactType(final Class dropClass) {
		DropPredicate result = f_exactType.get(dropClass);
		if (result == null) {
			result = new DropPredicate() {
				public boolean match(Drop d) {
					return d.getClass().equals(dropClass);
				}
			};
			f_exactType.put(dropClass, result);
		}
		return result;
	}
}
