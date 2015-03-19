package com.surelogic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.LibResources;
import com.surelogic.common.ref.IDecl.Kind;
public final class AnnotationUtil {
	public static Iterable<Class<?>> getApplicableAnnos(Kind kind) {
		final List<Class<?>> result = new ArrayList<Class<?>>();
		final ElementType type = mapToElementType(kind);
		if (type == null) {
			return Collections.emptyList();
		}
		for(Class<?> c : LibResources.getPromiseClassesWithoutMultipleAnnotationPromises()) {
			if (includesElementType(c, type)) {
				result.add(c);
			}
		}
		return result;
	}
	
	private static boolean includesElementType(final Class<?> c, final ElementType type) {
		final Target target = c.getAnnotation(Target.class);
		if (target == null) {
			return false;
		}
		for(final ElementType elt : target.value()) {
			if (elt == type) {
				return true;
			}
			if (elt == ElementType.TYPE && type == ElementType.ANNOTATION_TYPE) {
				return true;
			}
		}

		return false;
	}

	private static ElementType mapToElementType(Kind kind) {
		switch (kind) {
		case ANNOTATION:
			return ElementType.ANNOTATION_TYPE; // Also a TYPE
		case CLASS:
			return ElementType.TYPE;
		case CONSTRUCTOR:
			return ElementType.CONSTRUCTOR;
		case ENUM:
			return ElementType.TYPE;
		case FIELD:
			return ElementType.FIELD;
		case INTERFACE:
			return ElementType.TYPE;
		case METHOD:
			return ElementType.METHOD;
		case PACKAGE:
			return ElementType.PACKAGE;
		case PARAMETER:
			return ElementType.PARAMETER;
		case INITIALIZER:
		case LAMBDA:
		case TYPE_PARAMETER:			
			return null; // Nothing maps to this?
		default:
			throw new IllegalStateException("Unknown kind: "+kind);
		}
	}
	
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
