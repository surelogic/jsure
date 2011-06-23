package com.surelogic.jsure.core.xml;

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
	
	public static String translateParameters(IMethod m) throws JavaModelException {
		StringBuilder sb = new StringBuilder();
		for(String t : m.getParameterTypes()) {
			if (sb.length() != 0) {
				sb.append(',');
			}		
			translateParameter(m, sb, t);
		}
		return sb.toString();
	}	
	
	public static void translateParameter(IMethod m, StringBuilder sb, String t) throws JavaModelException {
		String mapped = typeMapping.get(t);
		if (mapped == null) {
			int dims = 0;
			while (t.charAt(dims) == Signature.C_ARRAY) {
				dims++;
			}
			if (dims == 0) {
				// Assumed to be a class name
				mapped = t.substring(1, t.length()-1).replace('$', '.');
				translatePossibleGenericType(m, sb, translateToRawType(mapped));				
			} else {
				translateParameter(m, sb, t.substring(dims));
				for(int i=0; i<dims; i++) {
					sb.append("[]");
				}
			}			
		} else {
			sb.append(mapped);
		}
	}
	
	private static void translatePossibleGenericType(IMethod m, StringBuilder sb, String t) throws JavaModelException {
		for(ITypeParameter p : m.getTypeParameters()) {
			if (t.equals(p.getElementName())) {
				translateToRawType(m, sb, p);
				return;
			}
		}
		IType type = m.getDeclaringType();
		while (type != null) {
			for(ITypeParameter p : type.getTypeParameters()) {
				if (t.equals(p.getElementName())) {
					translateToRawType(m, sb, p);
					return;
				}
			}
			type = type.getDeclaringType();
		}
		if (t.length() == 1) {
			System.out.println("Suspicious type: "+t);
		}
		sb.append(t);
	}
	
	private static void translateToRawType(IMethod m, StringBuilder sb, ITypeParameter p) throws JavaModelException {
		String[] bounds = p.getBounds();
		if (bounds.length == 0) {
			sb.append("java.lang.Object");
			return;
		}
		int i=0;
		if (bounds.length > 1 && bounds[0].equals("java.lang.Object")) {
			i = 1;
		} 
		translatePossibleGenericType(m, sb, translateToRawType(bounds[i]));		
	}
	
	private static String translateToRawType(String t) {
		int angleBracket = t.indexOf('<');
		if (angleBracket >= 0) {
			// Throw away type parameters
			return t.substring(0, angleBracket);
		}
		return t;
	}
	
	public static PackageElement makeModel(String pkg, String type) throws JavaModelException {
		final IType t = JDTUtility.findIType(null, pkg, type);
		return makeModel(t);
	}
	
	public static PackageElement makeModel(IType t) throws JavaModelException {
		if (t == null) {
			return null;
		}
		// TODO not quite right for nested classes
		final ClassElement c = new ClassElement(t.getElementName());
		for(IMethod m : t.getMethods()) {
			if (m.getDeclaringType().equals(t)) {
				if ("<clinit>".equals(m.getElementName())) {
					c.addMember(new ClassInitElement());
				} else {
					String params = translateParameters(m);									
					c.addMember(m.isConstructor() ? new ConstructorElement(params) : 
						                            new MethodElement(m.getElementName(), params));
				}
			}
		}
		// TODO fields
		// TODO nested classes -- only public ones?
		return new PackageElement(t.getPackageFragment().getElementName(), c);
	}
}
