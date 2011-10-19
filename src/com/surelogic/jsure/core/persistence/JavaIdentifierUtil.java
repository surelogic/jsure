package com.surelogic.jsure.core.persistence;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.*;

import com.surelogic.persistence.JavaIdentifier;

/**
 * Contains code to help decode and find JDT structures using JavaIdentifiers
 * 
 * @author Edwin
 */
public class JavaIdentifierUtil {
	/**
	 * project:pkg:type.inner:name:(params)
	 */
	public static String encodeBinding(IMethodBinding mb) {		
		final StringBuilder sb = new StringBuilder();
		encodeTypeBinding(sb, mb.getDeclaringClass());
		sb.append(JavaIdentifier.SEPARATOR).append(mb.getName());
		sb.append(JavaIdentifier.SEPARATOR).append('(');
		
		boolean first = true;
		for(ITypeBinding t : mb.getParameterTypes()) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}			
			sb.append(encodeParameterType(t));
		}
		sb.append(')');
		return sb.toString();
	}

	private static void encodeTypeBinding(final StringBuilder sb, ITypeBinding tb) {
		ITypeBinding enclosingTB = tb.getDeclaringClass();
		if (enclosingTB != null) {
			// Nested type
			encodeTypeBinding(sb, enclosingTB);
			sb.append('.').append(tb.getErasure().getName());
		} else {
			IMethodBinding mb = tb.getDeclaringMethod();
			if (mb != null) {
				throw new UnsupportedOperationException("Can't handle types declared within a method");
			} else {
				// Top level type				
				final IJavaElement e = tb.getJavaElement();
				if (e == null) {
					throw new IllegalStateException("Couldn't find a project for "+tb);
				}
				sb.append(e.getJavaProject().getElementName());
				sb.append(JavaIdentifier.SEPARATOR).append(tb.getPackage().getName());
				sb.append(JavaIdentifier.SEPARATOR).append(tb.getErasure().getName());
			}
		}
	}
	
	private static String encodeParameterType(ITypeBinding t) {
		// TODO Auto-generated method stub
		return t.getQualifiedName();
	}
	
	// TODO map a JavaIdentifier to a ???
}
