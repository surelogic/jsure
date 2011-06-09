package com.surelogic.jsure.core.xml;

import java.lang.annotation.Annotation;
import java.util.*;

import org.eclipse.jdt.core.*;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.xml.*;

public class PromisesXMLBuilder {
	private static final Map<String,String> typeMapping = new HashMap<String, String>();
	static {
		typeMapping.put(Signature.SIG_BOOLEAN, "boolean");
		typeMapping.put(Signature.SIG_BYTE, "byte");
		typeMapping.put(Signature.SIG_CHAR, "char");
		typeMapping.put(Signature.SIG_SHORT, "short");
		typeMapping.put(Signature.SIG_INT, "int");
		typeMapping.put(Signature.SIG_LONG, "long");
		typeMapping.put(Signature.SIG_FLOAT, "float");
		typeMapping.put(Signature.SIG_DOUBLE, "double");
	}
	
	public static String translateParameters(IMethod m) {
		StringBuilder sb = new StringBuilder();
		for(String t : m.getParameterTypes()) {
			if (sb.length() != 0) {
				sb.append(',');
			}		
			translateParameter(sb, t);
		}
		return sb.toString();
	}
	
	public static void translateParameter(StringBuilder sb, String t) {
		String mapped = typeMapping.get(t);
		if (mapped == null) {
			int dims = 0;
			while (t.charAt(dims) == Signature.C_ARRAY) {
				dims++;
			}
			if (dims == 0) {
				// Assumed to be a class name
				mapped = t.substring(1, t.length()-1).replace('$', '.');
			} else {
				translateParameter(sb, t.substring(dims));
				for(int i=0; i<dims; i++) {
					sb.append("[]");
				}
			}			
		}
		sb.append(mapped);
	}
	
	public static PackageElement makeModel(String pkg, String type) throws JavaModelException {
		final IType t = JDTUtility.findIType(null, pkg, type);
		// TODO not quite right for nested classes
		final ClassElement c = new ClassElement(type);
		for(IMethod m : t.getMethods()) {
			if (m.getDeclaringType().equals(t)) {
				if ("<clinit>".equals(m.getElementName())) {
					c.addMember(new ClassInitElement());
				} else {
					
				}
			}
		}
		return new PackageElement(pkg, c);
	}
}
