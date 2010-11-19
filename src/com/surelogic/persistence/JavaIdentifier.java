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
		final String id = JJNode.getInfo(decl);
		sb.append(id);

		final Operator op = JJNode.tree.getOperator(decl);
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
		final StringTokenizer st = new StringTokenizer(":");
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
		final IRNode type  = p.getTypeEnv().findNamedType(pkg+'.'+types);
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
	
	public static boolean isImpliedByPromise(IRNode decl, String annoType, String contents) {
		IAnnotationParseRule<?,?> r = PromiseFramework.getInstance().getParseDropRule(annoType);
		MinimalContext c = new MinimalContext(decl, '@'+annoType+' '+contents);
		if (r.parse(c, contents) == ParseResult.OK) {
			if (c.created.size() != 1) {
				throw new IllegalStateException("Created the wrong number of AASTs: "+c.created.size());
			}
			// Compare to the promise
			final IAASTRootNode aast = c.created.get(0);
			for(PromiseDrop<?> d : r.getStorage().getDrops(decl)) {
				// TODO can there be more than one?
				return d.getAST().implies(aast);
			}			
		}
		return false; 
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
