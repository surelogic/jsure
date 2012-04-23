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
			translateParameter(m, sb, t);
		}
		return sb.toString();
	}	
	
	private static boolean isInternalParameter(String t) {
		int i=0;
		while ((i = t.indexOf('$', i)) > 0) {
			if (Character.isDigit(t.charAt(i+1))) {
				// Can't be from source, so it must be generated				
				return true;
			}
			i++;
		}
		return false;
	}
	
	private static void translateParameter(IMethod m, StringBuilder sb, String t) throws JavaModelException {
		String mapped = typeMapping.get(t);
		if (mapped == null) {
			int dims = 0;
			while (t.charAt(dims) == Signature.C_ARRAY) {
				dims++;
			}
			if (dims == 0) {
				// Assumed to be a class name
				if (isInternalParameter(t)) {
					return;
				}
				if (sb.length() != 0) {
					sb.append(',');
				}		
				mapped = t.substring(1, t.length()-1).replace('$', '.');
				translatePossibleGenericType(m, sb, translateToRawType(mapped));				
			} else {
				translateParameter(m, sb, t.substring(dims));
				for(int i=0; i<dims; i++) {
					sb.append("[]");
				}
			}			
		} else {
			if (sb.length() != 0) {
				sb.append(',');
			}		
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
		ClassElement c = makeClass(t, false);
		if (c == null) {
			return null;
		}
		return new PackageElement(t.getPackageFragment().getElementName(), 0, c);
	}
	
	private static ClassElement makeClass(IType t, boolean isNested) throws JavaModelException {
		if (t == null) {
			return null;
		}
		if (t.isMember()) {
			isNested = true;
		}
		final ClassElement c = isNested ? new NestedClassElement(t.getElementName()) : new ClassElement(t.getElementName());
		for(IMethod m : t.getMethods()) {
			if (m.getElementName().contains("$") || Flags.isPrivate(m.getFlags())) {
				continue;
			}
			if (m.getDeclaringType().equals(t)) {
				if ("<clinit>".equals(m.getElementName())) {
					continue;
					//c.addMember(new ClassInitElement());
				} else {
					String params = translateParameters(m);		
					AbstractFunctionElement func = m.isConstructor() ? new ConstructorElement(params) : 
                            new MethodElement(m.getElementName(), params);
					c.addMember(func);
					makeParameters(m, func);
				}
			}
		}
		// TODO fields
		for(IType n : t.getTypes()) {
			if (Flags.isPrivate(n.getFlags())) {
				continue;
			}
			ClassElement ne = makeClass(n, true);
			c.addMember((IClassMember) ne);
		}
		return c;
	}
	
	private static void makeParameters(IMethod m, AbstractFunctionElement func) {
		for(int i=0; i<m.getNumberOfParameters(); i++) {
			if (isInternalParameter(m.getParameterTypes()[i])) {
				continue;
			}
			func.setParameter(new FunctionParameterElement(i));			
		}		 
	}

	/**
	 * Update to add Java elements
	 * 
	 * @return true if updated
	 */
	public static boolean updateElements(PackageElement p) throws JavaModelException {
		if (p.getClassElement() == null) {
			return false;
		}
		final IType t = JDTUtility.findIType(null, p.getName(), p.getClassElement().getName());
		final ClassElement c = makeClass(t, false);
		if (c == null) {
			return false;
		}
		final MergeResult<ClassElement> result = p.getClassElement().merge(c, MergeType.JAVA);
		return result.isModified;
	}

	public static PackageElement makePackageModel(String pkg) {
		return new PackageElement(pkg, 0, null);
	}
}
