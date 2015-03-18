package com.surelogic.annotation;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import com.surelogic.common.AnnotationConstants;
public final class AnnotationUtil {
	private static final String[] noArgMethods = new String[] { "toString",
		"hashCode", "annotationType", };

	private static final ConcurrentMap<String, Map<String, Attribute>> attrsByPromise = new ConcurrentHashMap<String, Map<String, Attribute>>();

	/**
	 * @return a map of attributes and default values
	 */
	public static Map<String, Attribute> getAttributes(String tag) {
		Map<String, Attribute> m = attrsByPromise.get(tag);
		if (m == null) {
			// This is idempotent, so it doesn't really matter which gets used
			m = computeAttributes(tag);
			attrsByPromise.putIfAbsent(tag, m);
		}
		return m;
	}

	private static Map<String, Attribute> computeAttributes(String tag) {
		final String qname = AnnotationConstants.PROMISE_PREFIX + tag;
		try {
			Class<?> cls = Class.forName(qname);
			Map<String, Attribute> l = new HashMap<String, Attribute>();

			outer: for (Method m : cls.getMethods()) {
				if (m.getParameterTypes().length > 0) {
					// Not an attribute
					continue outer;
				}
				for (String name : noArgMethods) {
					if (name.equals(m.getName())) {
						continue outer;
					}
				}
				/*
				 * if (AnnotationConstants.VALUE_ATTR.equals(m.getName())) {
				 * continue outer; }
				 */
				// System.out.println("Attribute '"+m.getName()+"' can appear on "+tag);
				Object value = m.getDefaultValue();
				Class<?> type = m.getReturnType();
				if (m.getReturnType().equals(String[].class)) {
					String[] values = (String[]) value;
					type = String.class;
					value = flattenStringArray(values);
				}
				l.put(m.getName(),
						new Attribute(m.getName(), type,
								value == null ? null : value.toString()));
			}
			return Collections.unmodifiableMap(l);
		} catch (ClassNotFoundException e) {
			// Ignore it
		}
		return Collections.emptyMap();
	}	


	public static String flattenStringArray(String[] values) {
		if (values == null || values.length == 0) {
			return "";
		} else {
			StringBuilder b = new StringBuilder();
			for(String v : values) {
				if (b.length() != 0) {
					b.append(", ");
				}
				b.append(v);
			}
			return b.toString();
		}
	}	
}
