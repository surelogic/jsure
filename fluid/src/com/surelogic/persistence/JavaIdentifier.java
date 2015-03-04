package com.surelogic.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.IIRProjects;
import com.surelogic.annotation.AbstractAnnotationParsingContext;
import com.surelogic.annotation.AnnotationLocation;
import com.surelogic.annotation.AnnotationSource;
import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.ParseResult;
import com.surelogic.annotation.parse.ScopedPromiseAdaptor;
import com.surelogic.annotation.parse.ScopedPromiseParse;
import com.surelogic.annotation.rules.ThreadEffectsRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.StartsPromiseDrop;
import com.surelogic.java.persistence.JavaIdentifierConstants;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedTypeDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class JavaIdentifier implements JavaIdentifierConstants {
	public static void testFindEncoding(IIRProjects projs, final IIRProject proj, IRNode cu) {
		for(IRNode tt : VisitUtil.getTypeDecls(cu)) {
			testFindEncodingType(projs, proj, tt);
		}
	}
	
	private static void testFindEncodingType(IIRProjects projs, final IIRProject proj, IRNode type) {
		testFindEncodingDecl(projs, proj, type);
		for(IRNode member : VisitUtil.getClassBodyMembers(type)) {
			final Operator op = JJNode.tree.getOperator(member);
			if (NestedTypeDeclaration.prototype.includes(op)) {
				testFindEncodingType(projs, proj, member);
			} else if (FieldDeclaration.prototype.includes(op)) {
				for(IRNode vd : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(member))) {
					testFindEncodingDecl(projs, proj, vd);
				}
			} else {
				testFindEncodingDecl(projs, proj, member);
			}			
		}
	}
	
	private static void testFindEncodingDecl(IIRProjects projs, final IIRProject proj, IRNode decl) {
		final String encoding = encodeDecl(proj, decl);
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
			encodeDecl(proj, decl);
		} else {
			//System.out.println("Found "+encoding);
		}
		final String target = createTarget(decl);
		if (target.endsWith(" 1")) {
			return; // A generated class
		}
		if (!matchesDecl(decl, target)) {
			System.err.println("Not matching target: "+target);
			matchesDecl(decl, target);
		}
		if (ThreadEffectsRules.startsNothing(decl)) {
			StartsPromiseDrop d0 = ThreadEffectsRules.getStartsSpec(decl);
			PromiseDrop<?> pd    = findMatchingPromise(decl, "Starts", "nothing");
			if (pd == null || !d0.equals(pd)) {
				System.err.println("No matching promise: "+d0);
				findMatchingPromise(decl, "Starts", "nothing");
			}
		}
	}
	
	private static final String DEFAULT_PKG = "(default)";
	
	public static String omitProject(String id) {
		if (id == null) {
			return null;
		}
		final int sep = id.indexOf(JavaIdentifier.SEPARATOR);
		if (sep > 0) {
			return id.substring(sep);
		}
		return id;
	}

	public static String isolateType(String id) {
		if (id == null) {
			return null;
		}
		final int paramStart = id.lastIndexOf(JavaIdentifier.SEPARATOR+'(');
		if (paramStart > 0) {
			final int sep = id.lastIndexOf(JavaIdentifier.SEPARATOR, paramStart);
			if (sep > 0) {
				return id.substring(0, sep);
			}
		}
		return id;
	}
	
	/**
	 * project:pkg:type.inner:name:(params)
	 */
	public static String encodeDecl(IRNode decl) {
		return encodeDecl(null, decl);
	}
	
	public static String encodeDecl(IIRProject proj, IRNode decl) {
		final IRNode type      = VisitUtil.getClosestType(decl);
		final IRNode cu        = VisitUtil.getEnclosingCompilationUnit(type == null ? decl : type);
		final StringBuilder sb = new StringBuilder();
		
		if (proj == null) {
			proj = Projects.getProject(cu);

			if (proj == null) {
				throw new IllegalStateException("No project found for "+JJNode.tree.getOperator(decl).name()+" : "+DebugUnparser.toString(decl));
			}
		}
		final IBinder b = proj.getTypeEnv().getBinder();
		sb.append(proj.getName()).append(SEPARATOR);

		final String pkg = VisitUtil.getPackageName(cu);
		if (pkg.equals("")) {
			sb.append(DEFAULT_PKG).append(SEPARATOR);
		} else {
			sb.append(pkg).append(SEPARATOR);
		}
		
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
		} else if (ParameterDeclaration.prototype.includes(op)) {
			IRNode func = VisitUtil.getEnclosingClassBodyDecl(decl);
			id = JJNode.getInfoOrNull(func);
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
	
	public static String extractType(String code) {
		String[] parts = code.split(SEPARATOR);		
		if (parts.length < 3) {
			throw new IllegalArgumentException("Bad encoding: "+code);
		}
		return parts[0]+SEPARATOR+parts[1]+SEPARATOR+parts[2];
	}
	
	public static String extractDecl(String typePrefix, String code) {
		String[] parts = code.split(SEPARATOR);		
		if (parts.length == 3) {
			return parts[2];
		}
		if (parts.length < 4) {
			throw new IllegalArgumentException("Bad encoding: "+code);
		}
		StringBuffer sb = new StringBuffer();
		if (parts[2].startsWith(typePrefix)) {
			if (parts[2].length() > typePrefix.length()) {
				sb.append(parts[2].substring(typePrefix.length()+1)).append('.');
			}
		} else {
			sb.append(parts[2]).append('.');			
		}
		for(int i=3; i<parts.length && i<5; i++) {
			sb.append(parts[i]);
		}
		return sb.toString();
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
		String tmp = st.nextToken();
		final String pkg   = DEFAULT_PKG.equals(tmp) ? "" : tmp;
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
			if (pkg.equals("")) {
				return p.getTypeEnv().findNamedType(types);
			}
			return p.getTypeEnv().findNamedType(pkg+'.'+types);
		} 
		// A nested type, so find the outer type
		final IRNode type;
		if (pkg.equals("")) {
			type = p.getTypeEnv().findNamedType(types.substring(0, firstDot));
		} else {
			type = p.getTypeEnv().findNamedType(pkg+'.'+types.substring(0, firstDot));
		}
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
			if (TypeDeclaration.prototype.includes(op)) {
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
			if (TypeDeclaration.prototype.includes(op)) {
				continue;
			}
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
	 * For testing matchesDecl()
	 */
	public static String createTarget(final IRNode decl) {
		final Operator op = JJNode.tree.getOperator(decl);
		final int mods    = getModifiers(decl, op);		
		if (mods == JavaNode.ALL_FALSE) {
			return getCorrespondingWildcard(decl, op);
		}
		StringBuilder sb  = new StringBuilder(); 
		if (JavaNode.isSet(mods, JavaNode.PUBLIC)) {
			sb.append("public");
		}
		else if (JavaNode.isSet(mods, JavaNode.PROTECTED)) {
			sb.append("protected");
		}
		else if (JavaNode.isSet(mods, JavaNode.PRIVATE)) {
			sb.append("private");
		}
		if (EnumConstantDeclaration.prototype.includes(op)) {
			sb.append(" static final");
		}
		else if (AnnotationElement.prototype.includes(op)) {
			sb.append(" final");
		}
		else if (!ConstructorDeclaration.prototype.includes(op)) {
			if (JavaNode.isSet(mods, JavaNode.STATIC)) {
				sb.append(" static");
			} else {
				sb.append(" !static");
			}		
			if (JavaNode.isSet(mods, JavaNode.FINAL)) {
				sb.append(" final");
			} else {
				sb.append(" !final");
			}
		}
		sb.append(' ').append(getCorrespondingWildcard(decl, op));
		return sb.toString();
	}
	
	public static int getModifiers(IRNode decl, Operator op) {
		if (VariableDeclarator.prototype.includes(op)) {
			return VariableDeclarator.getMods(decl);
		}
		return JavaNode.getModifiers(decl);
	}
	
	public static String getCorrespondingWildcard(IRNode n, Operator op) {
		if (VariableDeclarator.prototype.includes(op)) {
			return "* "+VariableDeclarator.getId(n); // field
		}
		else if (MethodDeclaration.prototype.includes(op)) {
			return MethodDeclaration.getId(n)+"(**)";
		}
		else if (ConstructorDeclaration.prototype.includes(op)) {
			return "new(**)";
		}
		else if (EnumConstantDeclaration.prototype.includes(op)) {
			return "* "+EnumConstantDeclaration.getId(n); 
		}
		else if (TypeDeclaration.prototype.includes(op)) {
			return JJNode.getInfo(n);
		}
		else if (AnnotationElement.prototype.includes(op)) {
			return "* "+AnnotationElement.getId(n); 
		}
		System.err.println("Unexpected op: "+op.name());
		return "* *";
	}
	
	/**
	 * Reuses the scoped promise target syntax to check if the
	 * declaration matches
	 */
	public static boolean matchesDecl(final IRNode decl, final String unparsedTarget) {
		try {
			
			Object tn = ScopedPromiseParse.prototype.initParser(unparsedTarget).promiseTarget().getTree();
			final ScopedPromiseAdaptor.Node node = (ScopedPromiseAdaptor.Node) tn;
			final MinimalContext c;
			if (VariableDeclarator.prototype.includes(decl)) {
				IRNode member = VisitUtil.getClosestClassBodyDecl(decl);
				c = new MinimalContext(member, unparsedTarget);
			} else {
				c = new MinimalContext(decl, unparsedTarget);
			}
			//node.useText(c);
			
			//final PromiseTargetNode target = (PromiseTargetNode) 
				node.finalizeAST(c);
			//return target.matches(c.getNode());
			throw new UnsupportedOperationException();
		} catch (Exception e) {
			e.printStackTrace();
			//throw new IllegalStateException("While parsing: "+unparsedTarget, e);
			return false;
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
				if (useImplication ? d.getAAST().implies(aast) : d.getAAST().isSameAs(aast)) {
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
			super(AnnotationSource.XML, AnnotationOrigin.DECL);
			decl = n;
			unparsedTarget = target;
		}
		
		@Override
		protected IRNode getAnnoNode() {
			return decl;
		}
		@Override
    public Operator getOp() {
			return JJNode.tree.getOperator(decl);
		}
		@Override
    public <T extends IAASTRootNode> void reportAAST(int offset, AnnotationLocation loc, Object o, T ast) {
			created.add(ast);
		}
		@Override
    public void reportErrorAndProposal(int offset, String msg, ProposedPromiseDrop.Builder proposal, String... moreInfo) {
			throw new IllegalStateException("While matching: "+msg);
		}
		@Override
    public void reportException(int offset, Exception e) {
			throw new IllegalStateException("While matching: "+unparsedTarget, e);
		}
	}
	/*
	private static String encodeDecl(String project, IJavaRef ref) {
		final IDecl decl = ref.getDeclaration();
		final IJavaRef.Position pos = ref.getPositionRelativeToDeclaration();
		if (project == null || decl == null || pos == IJavaRef.Position.WITHIN_DECL) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		switch (pos) {
		case ON_RECEIVER:
		case ON_RETURN_VALUE:
			sb.append(ref.getPositionRelativeToDeclaration()).append(SEPARATOR);
		default:
			break;
		}
		sb.append(project).append(SEPARATOR);
		sb.append(DeclUtil.getPackageNameOrEmpty(decl));
		String types = DeclUtil.getTypeNameDollarSignOrNull(decl);
		if (types == null) {
			return sb.toString();
		}
		sb.append(SEPARATOR).append(types);
		
		switch (decl.getKind()) {
		case CLASS:
		case ENUM:
		case INTERFACE:
			return sb.toString();
		case CONSTRUCTOR:
		case METHOD:
			sb.append(SEPARATOR).append(decl.getName());
			sb.append(SEPARATOR);
			sb.append(DeclUtil.getParametersFullyQualifiedOf((IDeclFunction) decl));
			break;
		case FIELD:
			sb.append(SEPARATOR).append(decl.getName());
			break;
		case PARAMETER:
			IDeclFunction func = (IDeclFunction) decl.getParent();
			sb.append(SEPARATOR).append(func.getName());
			sb.append(SEPARATOR);
			sb.append(DeclUtil.getParametersFullyQualifiedOf(func));
			sb.append(SEPARATOR).append(decl.getName());
			break;
		default:
			return null;
		}
		return sb.toString();
	}
	*/
}
