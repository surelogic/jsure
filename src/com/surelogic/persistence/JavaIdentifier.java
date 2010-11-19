package com.surelogic.persistence;

import java.util.*;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.*;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public final class JavaIdentifier {
	private static final String SEPARATOR = ":";
	
	public static void testFindEncoding(IIRProjects projs, IBinder b, IRNode cu) {
		for(IRNode tt : VisitUtil.getTypeDecls(cu)) {
			testFindEncodingType(projs, b, tt);
		}
	}
	
	private static void testFindEncodingType(IIRProjects projs, IBinder b, IRNode type) {
		testFindEncodingDecl(projs, b, type);
		for(IRNode member : VisitUtil.getClassBodyMembers(type)) {
			final Operator op = JJNode.tree.getOperator(member);
			if (NestedTypeDeclaration.prototype.includes(op)) {
				testFindEncodingType(projs, b, member);
			} else if (FieldDeclaration.prototype.includes(op)) {
				for(IRNode vd : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(member))) {
					testFindEncodingDecl(projs, b, vd);
				}
			} else {
				testFindEncodingDecl(projs, b, member);
			}			
		}
	}
	
	private static void testFindEncodingDecl(IIRProjects projs, IBinder b, IRNode decl) {
		final String encoding = encodeDecl(b, decl);
		if (encoding == null) {
			return; // Not meant to be encoded
		}
		IRNode found = findDecl(projs, encoding);
		if (!decl.equals(found)) {
			if (found == null) {
				System.err.println("Null match: "+encoding);
			} else {
				System.err.println("Not matching: "+encoding+" => "+DebugUnparser.toString(found));
			}
			findDecl(projs, encoding);
			encodeDecl(b, decl);
		} else {
			//System.out.println("Found "+encoding);
		}
	}
	
	public static String encodeDecl(IBinder b, IRNode decl) {
		final IRNode type      = VisitUtil.getClosestType(decl);
		final IRNode cu        = VisitUtil.getEnclosingCompilationUnit(type);
		final StringBuilder sb = new StringBuilder();
		sb.append(JavaProjects.getProject(cu).getName()).append(SEPARATOR);

		final String pkg   = VisitUtil.getPackageName(cu);
		sb.append(pkg).append(SEPARATOR);

		final String types = JavaNames.getRelativeTypeName(type);
		sb.append(types);
		if (type == decl) {			
			return sb.toString();			
		}
		sb.append(SEPARATOR);
		
		// A class body member
		final Operator op = JJNode.tree.getOperator(decl);
		final String id;
		if (ClassInitializer.prototype.includes(op)) {
			/*
			if (JavaNode.getModifier(decl, JavaNode.STATIC)) {
				id = "<clinit>";
			} else {
			*/
				return null;
			//}
		} else {
			id = JJNode.getInfoOrNull(decl);
			if (id == null) {
				System.out.println("Null id for "+op.name());
			}
		}
		sb.append(id);


		final IRNode method;
		final Operator methodOp;
		if (ParameterDeclaration.prototype.includes(op)) {
			method = VisitUtil.getEnclosingClassBodyDecl(decl);
			methodOp = JJNode.tree.getOperator(method);
		}
		else if (isNonFunction(op)) {
			return sb.toString();
		}
		else {
			method = decl;
			methodOp = op;
		}
		sb.append(SEPARATOR);

		final IRNode params = getParams(method, methodOp);
		encodeParameters(b, params, sb);		
		
		if (method != decl) { // parameter
			sb.append(SEPARATOR);
			sb.append(ParameterDeclaration.getId(decl));
		}
		return sb.toString();
	}

	private static void encodeParameters(IBinder b, final IRNode params, final StringBuilder sb) {
		boolean first = true;
		sb.append('(');
		for(IRNode param : Parameters.getFormalIterator(params)) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}			
			sb.append(encodeTypeName(b, ParameterDeclaration.getType(param)));
		} 
		sb.append(')');
	}

	private static boolean isNonFunction(final Operator op) {
		return !SomeFunctionDeclaration.prototype.includes(op) || AnnotationElement.prototype.includes(op);
	}

	private static IRNode getParams(IRNode decl, final Operator op) {
		IRNode params;
		if (MethodDeclaration.prototype.includes(op)) {
			params = MethodDeclaration.getParams(decl);
		}
		else if (ConstructorDeclaration.prototype.includes(op)) {
			params = ConstructorDeclaration.getParams(decl);
		}
		else {
			throw new IllegalStateException("Unknown declaration: "+op.name());
		}
		return params;
	}

	private static String encodeTypeName(IBinder b, IRNode type) {
		// TODO hack w/o colons
		return DebugUnparser.toString(type);
	}
	
	public static IRNode findDecl(IIRProjects projs, String code) {
		final StringTokenizer st = new StringTokenizer(code, ":");
		if (!st.hasMoreTokens()) {
			return null;
		}
		final String pName = st.nextToken();
		final IIRProject p = projs.get(pName);
		if (p == null) {
			return null;
		}
		final String pkg   = st.nextToken();
		final String types = st.nextToken();
		final IRNode type  = findType(p, pkg, types);
		if (type == null) {
			return null;
		}
		if (!st.hasMoreTokens()) {
			return type;
		}
		final String id = st.nextToken();
		if (!st.hasMoreTokens()) {
			// Look for the class member
			return findNonFunctionMember(type, id);
		} else {
			// Look for the constructor/method
			final String params = st.nextToken();
			for(IRNode func : VisitUtil.getClassMethods(type)) {
				if (id.equals(JJNode.getInfoOrNull(func))) {
					StringBuilder sb = new StringBuilder();
					IRNode funcParams = getParams(func, JJNode.tree.getOperator(func));
					encodeParameters(p.getTypeEnv().getBinder(), funcParams, sb);					
					if (params.equals(sb.toString())) {
						return func;
					}
				}
			}
		}
		return null;
	}

	private static IRNode findType(IIRProject p, String pkg, String types) {
		final int firstDot = types.indexOf('.');
		if (firstDot < 0) {
			return p.getTypeEnv().findNamedType(pkg+'.'+types);
		} 
		// A nested type, so find the outer type
		final IRNode type = p.getTypeEnv().findNamedType(pkg+'.'+types.substring(0, firstDot));
		return findNestedTypeByQName(type, types.substring(firstDot+1));
	}

	private static IRNode findNestedTypeByQName(IRNode type, String types) {
		if (type == null) {
			return null;
		}
		final int firstDot = types.indexOf('.');
		if (firstDot < 0) {
			return findNestedType(type, types);
		} 
		final IRNode nextT = findNestedType(type, types.substring(0, firstDot));
		return findNestedTypeByQName(nextT, types.substring(firstDot+1));		
	}

	private static IRNode findNestedType(IRNode type, String id) {
		for(IRNode member : VisitUtil.getClassBodyMembers(type)) {
			final Operator op = JJNode.tree.getOperator(member);	
			if (NestedTypeDeclaration.prototype.includes(op)) {
				if (id.equals(JJNode.getInfoOrNull(member))) {
					return member;
				}
			}
		}
		return null;
	}
	
	private static IRNode findNonFunctionMember(final IRNode type, final String id) {
		for(IRNode member : VisitUtil.getClassBodyMembers(type)) {
			final Operator op = JJNode.tree.getOperator(member);			
			if (FieldDeclaration.prototype.includes(op)) {
				IRNode vdecls = FieldDeclaration.getVars(member);
				for(IRNode vdecl : VariableDeclarators.getVarIterator(vdecls)) {
					if (id.equals(JJNode.getInfoOrNull(vdecl))) {
						return vdecl;
					}
				}
			}
			else if (isNonFunction(op)) {
				if (id.equals(JJNode.getInfoOrNull(member))) {
					return member;
				}
			}
		}
		return null;
	}
	
	/**
	 * Reuses the scoped promise target syntax to check if the
	 * declaration matches
	 */
	public static boolean matchesDecl(final IRNode decl, final String unparsedTarget) {
		try {
			Object tn = ScopedPromiseParse.prototype.initParser(unparsedTarget).promiseTarget().getTree();
			final ScopedPromiseAdaptor.Node node = (ScopedPromiseAdaptor.Node) tn;
			MinimalContext c = new MinimalContext(decl, unparsedTarget);
			node.useText(c);
			
			final PromiseTargetNode target = (PromiseTargetNode) node.finalizeAST(c);
			return target.matches(decl);
		} catch (Exception e) {
			throw new IllegalStateException("While parsing: "+unparsedTarget, e);
		}
	}
	
	/**
	 * Mainly to find the matching "About" drops
	 */
	public static PromiseDrop<?> findMatchingPromise(IRNode decl, String annoType, String contents) {
		return matchPromise(decl, annoType, contents, false);
	}
	
	/**
	 * Mainly used to find "Check" drops that imply the required constraint
	 */
	public static PromiseDrop<?> isImpliedByPromise(IRNode decl, String annoType, String contents) {
		return matchPromise(decl, annoType, contents, true);
	}
	
	private static PromiseDrop<?> matchPromise(final IRNode decl, final String annoType, final String contents, 
			                                   final boolean useImplication) {
		IAnnotationParseRule<?,?> r = PromiseFramework.getInstance().getParseDropRule(annoType);		
		MinimalContext c = new MinimalContext(decl, '@'+annoType+' '+contents);
		if (r.parse(c, contents) == ParseResult.OK) {
			if (c.created.size() != 1) {
				throw new IllegalStateException("Created the wrong number of AASTs: "+c.created.size());
			}
			// Compare to the promise
			final IAASTRootNode aast = c.created.get(0);
			for(PromiseDrop<?> d : r.getStorage().getDrops(decl)) {
				if (useImplication ? d.getAST().implies(aast) : d.getAST().equals(aast)) {
					return d;
				}
			}
		}
		return null; 
	}
	
	private static class MinimalContext extends AbstractAnnotationParsingContext {
		final IRNode decl;
		final String unparsedTarget;
		final List<IAASTRootNode> created = new ArrayList<IAASTRootNode>(0);
		
		MinimalContext(IRNode n, String target) {
			super(AnnotationSource.XML);
			decl = n;
			unparsedTarget = target;
		}
		
		@Override
		protected IRNode getNode() {
			return decl;
		}
		public Operator getOp() {
			return JJNode.tree.getOperator(decl);
		}
		public <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, Object o, T ast) {
			created.add(ast);
		}
		public void reportError(int offset, String msg) {
			throw new IllegalStateException("While matching: "+msg);
		}
		public void reportException(int offset, Exception e) {
			throw new IllegalStateException("While matching: "+unparsedTarget, e);
		}
	}
}
