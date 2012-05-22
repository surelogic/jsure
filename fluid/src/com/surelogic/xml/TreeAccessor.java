/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/TreeAccessor.java,v 1.20 2008/10/27 13:58:44 chance Exp $*/
package com.surelogic.xml;

import java.util.Iterator;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

/**
 * Simple interface to access the fAST given an element type, name (or other
 * defining attribute), and top node. (Most of this code is take from the old
 * DOM model parser code)
 * 
 * @author Spencer.Whitman
 */
public final class TreeAccessor implements TestXMLParserConstants {
	private static final SyntaxTreeInterface tree = JJNode.tree;

	/**
	 * Method findClass.
	 */
	public static IRNode findNestedClass(String name, IRNode type) {
		//LOG.info("Looking for class " + name);
		if (type == null) {
			return null;
		}
		IRNode body = VisitUtil.getClassBody(type);
		for(IRNode m : ClassBody.getDeclIterator(body)) {
			Operator op = tree.getOperator(m);
			if (op instanceof TypeDeclInterface
					&& name.equals(TypeDeclaration.getId(m))) {
				return m;
			}
		}
		String desc = "Couldn't find class " + name + " in "
				+ JavaNames.getFullTypeName(type);
		//LOG.warning(desc);
		System.err.println("WARNING: "+desc);
		reportProblem(desc, type);
		return null;
	}

	/**
	 * Method findConstructor.
	 * 
	 * @param elt
	 * @param top
	 * @return IRNode
	 */
	public static IRNode findConstructor(final String params, IRNode top,
			ITypeEnvironment tEnv) {
		if (top == null) {
			return null;
		}
		final boolean verbose = LOG.isLoggable(Level.FINER);
		Iterator<IRNode> e = VisitUtil.getClassBodyMembers(top);
		if (verbose) {
			LOG.finer("Looking for constructor w/ params: " + params);
		}
		while (e.hasNext()) {
			IRNode c = e.next();
			Operator op = tree.getOperator(c);
			if (op instanceof ConstructorDeclaration) {
				if (verbose) {
					LOG.finer("Looking at "+JavaNames.genSimpleMethodConstructorName(c));
				}
				IRNode ps = ConstructorDeclaration.getParams(c);
				if (paramsMatch(Parameters.getFormalIterator(ps), params, tEnv)) {
					if (verbose) {
						LOG.finer("Found a match on params: " + params);
					}
					return c;
				}
				if (verbose) {
					LOG.finer("Looking at constructor, but no match on parameters: "
							+ JavaNames.genSimpleMethodConstructorName(c));
				}
			} else {
				// LOG.finer("Looking at "+op.name()+"
				// "+JavaNode.getInfoOrNull(c));
			}
		}
		String desc = "Couldn't find constructor with params (" + params
				+ ") in " + JavaNames.getFullTypeName(top);
		//LOG.warning(desc);
		System.err.println("WARNING: "+desc);
		reportProblem(desc, top);
		return null;
	}

	/**
	 * 
	 * @param name
	 * @param top
	 * @return
	 */
	public static IRNode findField(final String name, IRNode top) {
		if (top == null) {
			return null;
		}
		LOG.info("Looking for field " + name + " starting at node "
				+ DebugUnparser.toString(top));

		Iterator<IRNode> e = VisitUtil.getClassBodyMembers(top);
		while (e.hasNext()) {
			IRNode fd = e.next();
			if (tree.getOperator(fd) instanceof FieldDeclaration) {
				IRNode vds = FieldDeclaration.getVars(fd);
				Iterator<IRNode> vdecls = VariableDeclarators
						.getVarIterator(vds);
				if (vdecls.hasNext()) {
					IRNode vdecl = vdecls.next();
					if (vdecls.hasNext()) {
						LOG
								.warning("Found a decl w/ 1+, so might skip a real decl: "
										+ name);
					}
					if (name.equals(VariableDeclarator.getId(vdecl))) {
						if (vdecls.hasNext()) {
							LOG.warning("Promising more than intended for: "
									+ name);
						}
						LOG.finer("Matched field " + name);
						return fd;
					}
				}
			}
		}
		String desc = "Couldn't find field '" + name + "' in "
				+ JavaNames.getFullTypeName(top);
		LOG.warning(desc);
		reportProblem(desc, top);
		return null;
	}

	/**
	 * Method findParameter.(assumes that an index or name is given and that
	 * params are named arg0, arg1, ...)
	 * 
	 * @param elt
	 * @param top
	 * @return IRNode
	 */
	public static IRNode findParameter(String index, String name, IRNode top) {
		if (top == null) {
			return null;
		}
		Operator op = tree.getOperator(top);
		IRNode params = (op instanceof MethodDeclaration) ? MethodDeclaration
				.getParams(top) : ConstructorDeclaration.getParams(top);
		try {
			if (!(index == null)) {
				LOG.finer("Matched arg " + index);
				return Parameters.getFormal(params, Integer.parseInt(index));
			} else {
				LOG.finer("Matched arg " + name);
				return BindUtil.findLV(top, name);
			}
		} catch (Exception e) {
			String desc = "Couldn't find an arg "
					+ ((index != null) ? index : name) + " in "
					+ JavaNames.genQualifiedMethodConstructorName(top);
			LOG.log(Level.WARNING, desc, e);
			reportProblem(desc, top);
			return null;
		}
	}

	public static boolean isClassDeclaration(IRNode node) {
		return (tree.getOperator(node) instanceof ClassDeclaration);
	}

	public static IRNode findMethod(IRNode root, String name, String params,
			ITypeEnvironment tEnv) {
		if (root == null) {
			return null;
		}
		final boolean debug = LOG.isLoggable(Level.FINER);

		// XXX Problem is here

		Iterator<IRNode> e = VisitUtil.getClassBodyMembers(root);
		while (e.hasNext()) {
			IRNode m = e.next();
			Operator op = tree.getOperator(m);

			if (op instanceof MethodDeclaration
					&& name.equals(MethodDeclaration.getId(m))) {
				IRNode ps = MethodDeclaration.getParams(m);

				if (paramsMatch(Parameters.getFormalIterator(ps), params, tEnv)) {
					LOG.finer("Found a match for " + name + "(" + params + ")");
					return m;
				} else if (debug) {
					LOG.finer("Looking at method " + name
							+ ", but no match on parameters: " + params);
				}
			} else if (debug) {
				LOG.finer("Looking at " + op.name() + " "
						+ JJNode.getInfoOrNull(m));
			}
		}
		String desc = "Couldn't find " + name + "(" + (params == null ? "" : params) + ") in "
				+ JavaNames.getFullTypeName(root);
		//LOG.warning(desc);
		System.err.println("WARNING: "+desc);
		reportProblem(desc, root);
		return null;
	}

	private static boolean paramsMatch(Iterator<IRNode> ps, String params,
			ITypeEnvironment tEnv) {
		if (params == null || params.equals("")) {
			return !ps.hasNext();
		}
		final boolean finerIsLoggable = LOG.isLoggable(Level.FINER);

		String[] sa = params.split(", |,");
		// StringTokenizer st = new StringTokenizer(params, ", |,");
		int max = sa.length; // st.countTokens();
		// while (st.hasMoreTokens()) {
		for (int i = 0; i < sa.length; i++) {
			String p = sa[i];
			// String p = st.nextToken();
			if (!ps.hasNext()) {
				if (finerIsLoggable)
					LOG.finer("Unmatched parameter: " + p);
				return false;
			}
			IRNode p2 = ps.next();
			if ("*".equals(p)) {
				continue;
			}
			IJavaType t2 = tEnv.getBinder().getJavaType(p2);
			LOG.finer("Did not check parameter: " + p);

			if (t2 == null) {
				LOG
						.info("Unable to match parameter since type binding is null");
				return false;
			}

			IJavaType t1 = findJavaType(tEnv, p);
			if (t1 == null) {
				if (finerIsLoggable)
					LOG.info("Couldn't find type: " + p);
				return false;
			} else if (t2.equals(t1) || p.equals(t2.getName())) {
				if (finerIsLoggable)
					LOG.finer("Matched parameter: " + p);
			} else if (TypeErasure.calcTypeErasure(t2.getName(), tEnv).equals(
					t1.getName())) {
				if (finerIsLoggable)
					LOG.finer("Matched parameter: " + p);
			} else if (t2 instanceof IJavaDeclaredType
					&& t1 instanceof IJavaDeclaredType) {
				IJavaDeclaredType d1 = (IJavaDeclaredType) t1;
				IJavaDeclaredType d2 = (IJavaDeclaredType) t2;
				if (!d1.getDeclaration().equals(d2.getDeclaration())) {
					if (finerIsLoggable)
						LOG.finer("getDeclaration does not match for types t1 "
								+ t1.getName() + "and t2 " + t2.getName());
					return false;
				}
			} else if (matchesTypeErasure(t1, t2)) {
				if (finerIsLoggable)
					LOG.finer("Matched erasure of parameter: " + p);
			} else {
				if (finerIsLoggable)
					LOG.finer("Parameter not matched: " + t1.getName() + " vs "
							+ t2.getName());
				return false;
			}

		}
		if (ps.hasNext()) {
			if (finerIsLoggable)
				LOG.finer("Unmatched IR parameter");
			return false;
		}
		if (finerIsLoggable)
			LOG.finer("Matched all " + max + " params");
		return true;

	}

	private static IJavaType findJavaType(ITypeEnvironment tEnv, String name) {
		IJavaType t = findJavaTypeByName(tEnv, name);
		if (t != null) {
			return t;
		}
		final int lastDot = name.lastIndexOf('.');
		if (lastDot < 0) {
			return null;
		}
		// Try without the last name segment
		t = findJavaTypeByName(tEnv, name.substring(0, lastDot));
		if (t == null) {
			return null;
		}
		if (!(t instanceof IJavaDeclaredType)) {
			throw new IllegalStateException("No decl type for "+name);
		}
		// From AbstractTypeEnvironment.findJavaTypeByName()
		// Check for array dimensions
		final IJavaDeclaredType outer = (IJavaDeclaredType) t;
		final String lastSeg = name.substring(lastDot+1, name.length());
	    int dims = 0;    
	    int possibleArray = lastSeg.length() - 2;
	    while (possibleArray > 0 && lastSeg.charAt(possibleArray) == '[' && 
	    		                    lastSeg.charAt(possibleArray+1) == ']') {
	      dims++;
	      possibleArray -= 2;
	    }
	    possibleArray += 2;		
	    IRNode inner;
	    if (dims > 0) {
	    	inner = findNestedClass(lastSeg.substring(0, possibleArray), outer.getDeclaration());
	    } else {
	    	inner = findNestedClass(lastSeg, outer.getDeclaration());
	    }
	    IJavaType rv = tEnv.convertNodeTypeToIJavaType(inner);
	    if (dims > 0) {
	    	return JavaTypeFactory.getArrayType(rv, dims);
	    }
	    return rv;
	}

	private static IJavaType findJavaTypeByName(ITypeEnvironment tEnv, String p) {
		try {
			return tEnv.findJavaTypeByName(p);
		} catch (NullPointerException n) {
			return tEnv.findJavaTypeByName(TypeErasure.calcTypeErasure(p,
					tEnv));
		}
	}

	private static boolean matchesTypeErasure(IJavaType t1, IJavaType t2) {
		// FIX Hack to handle generic types
		if (t2 instanceof IJavaTypeFormal && t1 instanceof IJavaDeclaredType) {
			// FIX IJavaTypeFormal tf = (IJavaTypeFormal) t2;
			// FIX return t1.equals(tf.getExtendsBound());
			return true;
		} else if (t2 instanceof IJavaArrayType && t1 instanceof IJavaArrayType) {
			IJavaArrayType a1 = (IJavaArrayType) t1;
			IJavaArrayType a2 = (IJavaArrayType) t2;
			if (a1.getDimensions() == a2.getDimensions()) {
				return matchesTypeErasure(a1.getBaseType(), a2.getBaseType());
			}
			// FIX still could match if the base type is Object
		}
		return false;
	}

	private static void reportProblem(String desc, IRNode top) {
		PromiseFramework.getInstance().getReporter().reportProblem(desc, top);
	}

}
