package edu.cmu.cs.fluid.java.util;

import com.surelogic.common.Pair;
import com.surelogic.common.ref.*;
import com.surelogic.common.ref.Decl.DeclBuilder;
import com.surelogic.common.ref.TypeRef;
import com.surelogic.common.ref.Decl.*;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Builds an {@link IDecl} from an IRNode location
 * 
 * @author Edwin
 */
public class DeclFactory {
	private final IBinder binder;
	
	public DeclFactory(IBinder b) {
		if (b == null) {
			throw new IllegalArgumentException("null binder");
		}
		binder = b;
	}
	
	/**
	 * Constructs an {@link IDecl} from the passed node and returns if the node is
	 * on or within the declaration.
	 * 
	 * @param here
	 *          the node.
	 * @return a pair with non-{@code} null entries, or {@code null} to indicate
	 *         something went wrong.
	 */
	public Pair<IDecl, IDecl.Position> getDeclAndPosition(IRNode here) {
		if (here == null || here.identity() == IRNode.destroyedNode) {
			return null;
		}
		DeclBuilder b = buildDecl(here);
		if (b == null) {
			if (Declaration.prototype.includes(here) || AnonClassExpression.prototype.includes(here)) {
				buildDecl(here);
			}
			return null;
		}
		final IDecl decl = b.build();	
		if (decl == null) {
			return null;
		}
		final Operator op = JJNode.tree.getOperator(here);
		if (Declaration.prototype.includes(op) || op instanceof TypeDeclInterface) {
			return new Pair<IDecl, IDecl.Position>(decl, IDecl.Position.ON);
		}
		return new Pair<IDecl, IDecl.Position>(decl, IDecl.Position.WITHIN);
	}

	private DeclBuilder buildDecl(IRNode here) {
		if (here == null) {
			return null;
		}		
		final IRNode parent = JJNode.tree.getParentOrNull(here);
		DeclBuilder parentB = buildDecl(parent);
		final Operator op = JJNode.tree.getOperator(here);
		final DeclBuilder b;
		if (op instanceof TypeDeclInterface) {
			b = buildTypeDecl(here, (TypeDeclInterface) op, parentB);
			
			if (parentB == null) {
				IRNode cu = VisitUtil.getEnclosingCompilationUnit(here);
				IRNode pd = CompilationUnit.getPkg(cu);						
				parentB = buildNonTypeDecl(pd, (Declaration) JJNode.tree.getOperator(pd), null);
			}
		}
		else if (op instanceof Declaration) {
			Declaration d = (Declaration) op;
			if (ignoreNode(d, parent)) {
				// Ignore this parameter
				return parentB;
			}
			b = buildNonTypeDecl(here, d, parentB);
		}
		else {
			return parentB;
		}
		b.setParent(parentB);
		return b;
	}
	
	/**
	 * Filter out cases that show up because node types are reused
	 */
	private boolean ignoreNode(Declaration d, IRNode parent) {
		final Operator pop;
		switch (d.getKind()) {
		case PARAMETER:
			pop = JJNode.tree.getOperator(parent);
			return ForEachStatement.prototype.includes(pop) || CatchClause.prototype.includes(pop);
		case FIELD:
			pop = JJNode.tree.getOperator(parent);
			if (VariableResource.prototype.includes(pop)) {
				return true;
			}
			IRNode gparent = JJNode.tree.getParentOrNull(parent);			
			return !FieldDeclaration.prototype.includes(gparent);			
		default:
			return false;
		}
	}

	private IDecl.Visibility getVisibility(IRNode decl) {
		final int mods = JavaNode.getModifiers(decl);
		return getVisibility(mods);
	}
	
	private IDecl.Visibility getVisibility(final int mods) {
		if (JavaNode.isSet(mods, JavaNode.PUBLIC)) {
			return IDecl.Visibility.PUBLIC;
		}
		if (JavaNode.isSet(mods, JavaNode.PROTECTED)) {
			return IDecl.Visibility.PROTECTED;
		}
		if (JavaNode.isSet(mods, JavaNode.PRIVATE)) {
			return IDecl.Visibility.PRIVATE;
		}
		return IDecl.Visibility.DEFAULT;
	}
	
	private DeclBuilder buildTypeDecl(IRNode decl, TypeDeclInterface t, DeclBuilder parent) {
		final String name = JJNode.getInfoOrNull(decl);
		switch (t.getKind()) {
		case CLASS:
			ClassBuilder c = new ClassBuilder(name);
			if (t instanceof ClassDeclaration) {
				IRNode types = ClassDeclaration.getTypes(decl);
				int i = 0;
				for(IRNode typeParam : TypeFormals.getTypeIterator(types)) {
					TypeParameterBuilder tpb = buildTypeParameter(i, typeParam);
					c.addTypeParameter(tpb);
					i++;
				}				
				final int mods = JavaNode.getModifiers(decl);
				c.setIsAbstract(JavaNode.isSet(mods, JavaNode.ABSTRACT));			
				c.setIsFinal(JavaNode.isSet(mods, JavaNode.FINAL));	
				c.setIsStatic(JavaNode.isSet(mods, JavaNode.STATIC));					
				c.setVisibility(getVisibility(mods));
			} else {
				c.setVisibility(IDecl.Visibility.ANONYMOUS);
			}
			return c;
		case ENUM:
			EnumBuilder e = new EnumBuilder(name);
			e.setVisibility(getVisibility(decl));
			return e;
		case INTERFACE:
			InterfaceBuilder ib = new InterfaceBuilder(name);
			if (t instanceof InterfaceDeclaration) {
				IRNode types = InterfaceDeclaration.getTypes(decl);
				int i = 0;
				for(IRNode typeParam : TypeFormals.getTypeIterator(types)) {
					TypeParameterBuilder tpb = buildTypeParameter(i, typeParam);
					ib.addTypeParameter(tpb);
					i++;
				}	
			} 
			ib.setVisibility(getVisibility(decl));
			return ib;
		case TYPE_PARAMETER:
			final int num = computePosition(decl);
			//return buildTypeParameter(num, decl);			
			return parent.getTypeParameterBuilderAt(num);
		case FIELD: // EnumConstantClassDecl
			return buildNonTypeDecl(decl, (Declaration) t, parent);
		}		
		return null;
	}
	
	private TypeParameterBuilder buildTypeParameter(int i, IRNode t) {
		final String name = TypeFormal.getId(t);
		final TypeParameterBuilder b = new TypeParameterBuilder(i, name);
		final IRNode bounds = TypeFormal.getBounds(t);
		for(IRNode bound : MoreBounds.getBoundIterator(bounds)) {
			b.addBounds(computeTypeRef(bound));
		}
		return b;
	}

	private DeclBuilder buildNonTypeDecl(IRNode decl, Declaration d, DeclBuilder parent) {
		final String name = JJNode.getInfoOrNull(decl);
		IRNode params, types, type;
		int i = 0;
		switch (d.getKind()) {
		case CONSTRUCTOR:
			ConstructorBuilder c = new ConstructorBuilder();
			c.setVisibility(getVisibility(decl));
			
			params = ConstructorDeclaration.getParams(decl);
			for(IRNode param : Parameters.getFormalIterator(params)) {
				ParameterBuilder pb = buildParameter(param, i);
				c.addParameter(pb);
				i++;
			}	
			types = ConstructorDeclaration.getTypes(decl);
			i = 0;
			for(IRNode typeParam : TypeFormals.getTypeIterator(types)) {
				TypeParameterBuilder tpb = buildTypeParameter(i, typeParam);
				c.addTypeParameter(tpb);
				i++;
			}	
			return c;
		case FIELD:
			FieldBuilder f = new FieldBuilder(name);
			if (d instanceof EnumConstantDeclaration) {
				f.setIsFinal(true);
				f.setIsStatic(true);
				f.setVisibility(IDecl.Visibility.PUBLIC);
				type = VisitUtil.getEnclosingType(decl);
			} else {
				final int mods = VariableDeclarator.getMods(decl);		
				f.setIsFinal(JavaNode.isSet(mods, JavaNode.FINAL));	
				f.setIsStatic(JavaNode.isSet(mods, JavaNode.STATIC));					
				f.setVisibility(getVisibility(mods));	
				type = VariableDeclarator.getType(decl);
			}							
			f.setTypeOf(computeTypeRef(type));
			return f;
		case INITIALIZER:
			InitializerBuilder init = new InitializerBuilder();
			init.setIsStatic(JavaNode.getModifier(decl, JavaNode.STATIC));	
			return init;
		case METHOD:
			MethodBuilder m = new MethodBuilder(name);
			if (d instanceof MethodDeclaration) {
				final int mods = JavaNode.getModifiers(decl);
				m.setIsAbstract(JavaNode.isSet(mods, JavaNode.ABSTRACT));			
				m.setIsFinal(JavaNode.isSet(mods, JavaNode.FINAL));	
				m.setIsStatic(JavaNode.isSet(mods, JavaNode.STATIC));					
				m.setVisibility(getVisibility(mods));	
				
				params = MethodDeclaration.getParams(decl);
				for(IRNode param : Parameters.getFormalIterator(params)) {
					ParameterBuilder pb = buildParameter(param, i);
					m.addParameter(pb);
					i++;
				}	
				types = MethodDeclaration.getTypes(decl);
				i = 0;
				for(IRNode typeParam : TypeFormals.getTypeIterator(types)) {
					TypeParameterBuilder tpb = buildTypeParameter(i, typeParam);
					m.addTypeParameter(tpb);
					i++;
				}		
				type = MethodDeclaration.getReturnType(decl);
			} else { // AnnoElement
				m.setVisibility(IDecl.Visibility.PUBLIC);
				m.setIsAbstract(false);				
				m.setIsFinal(true);
				m.setIsStatic(false);
				type = AnnotationElement.getType(decl);
			}
			m.setReturnTypeOf(computeTypeRef(type));
			return m;
		case PACKAGE:
			return new PackageBuilder(name);			
		case PARAMETER:						
			final int num = computePosition(decl);
			//return buildParameter(decl, num, name);
			return parent.getParameterBuilderAt(num);
		}
		return null;
	}

	private ParameterBuilder buildParameter(IRNode decl, int num, String name) {
		ParameterBuilder param = new ParameterBuilder(num, name);
		param.setIsFinal(JavaNode.getModifier(decl, JavaNode.FINAL));	
		
		IRNode type = ParameterDeclaration.getType(decl);
		param.setTypeOf(computeTypeRef(type));
		return param;
	}

	private ParameterBuilder buildParameter(IRNode param, int i) {
		return buildParameter(param, i, JJNode.getInfoOrNull(param));
	}
	
	private int computePosition(IRNode child) {
		IRLocation loc = JJNode.tree.getLocation(child);
		IRNode parent = JJNode.tree.getParent(child);
		if (!child.equals(JJNode.tree.getChild(parent, loc))) {
			throw new IllegalStateException();
		}
		return JJNode.tree.childLocationIndex(parent, loc);
	}
	
	private TypeRef computeTypeRef(IRNode ref) {
		IJavaType t = binder.getJavaType(ref);
		if (t == null) {
			String unparse = DebugUnparser.toString(ref);
			return new TypeRef(unparse, unparse);
		}
		TypeRef r = new TypeRef(t.toFullyQualifiedText(), t.toSourceText());		
		return r;
	}
}
