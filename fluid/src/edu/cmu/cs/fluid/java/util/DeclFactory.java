package edu.cmu.cs.fluid.java.util;

import com.surelogic.common.Pair;
import com.surelogic.common.ref.Decl.AnnotationBuilder;
import com.surelogic.common.ref.Decl.ClassBuilder;
import com.surelogic.common.ref.Decl.ConstructorBuilder;
import com.surelogic.common.ref.Decl.DeclBuilder;
import com.surelogic.common.ref.Decl.EnumBuilder;
import com.surelogic.common.ref.Decl.FieldBuilder;
import com.surelogic.common.ref.Decl.InitializerBuilder;
import com.surelogic.common.ref.Decl.InterfaceBuilder;
import com.surelogic.common.ref.Decl.MethodBuilder;
import com.surelogic.common.ref.Decl.PackageBuilder;
import com.surelogic.common.ref.Decl.ParameterBuilder;
import com.surelogic.common.ref.Decl.TypeParameterBuilder;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.TypeRef;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.ForEachStatement;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.MoreBounds;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.TypeFormals;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableResource;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
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
  public Pair<IDecl, IJavaRef.Position> getDeclAndPosition(IRNode here) {
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
      return new Pair<IDecl, IJavaRef.Position>(decl, IJavaRef.Position.ON);
    }
    else if (ReceiverDeclaration.prototype.includes(op)) {
        return new Pair<IDecl, IJavaRef.Position>(decl, IJavaRef.Position.ON_RECEIVER);
    }
    else if (ReturnValueDeclaration.prototype.includes(op)) {
        return new Pair<IDecl, IJavaRef.Position>(decl, IJavaRef.Position.ON_RETURN_VALUE);
    }
    return new Pair<IDecl, IJavaRef.Position>(decl, IJavaRef.Position.WITHIN);
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
    } else if (op instanceof Declaration) {
      Declaration d = (Declaration) op;
      if (ignoreNode(d, parent)) {
        // Ignore this parameter
        return parentB;
      }
      b = buildNonTypeDecl(here, d, parentB);
    } else {
      return parentB;
    }
    b.setParent(parentB);
    return b;
  }

  public static IRNode findEnclosingDecl(IRNode here) {
	  if (here == null) {
		  return null;
	  }
	  final IRNode parent = JJNode.tree.getParentOrNull(here);
	  IRNode rv = findClosestDecl(parent);
	  if (rv == null && parent != null && TypeDeclaration.prototype.includes(here)) {
		  final IRNode gparent = JJNode.tree.getParentOrNull(parent);
		  return CompilationUnit.getPkg(gparent);
	  }
	  return rv;
  }
  
  public static IRNode findClosestDecl(IRNode here) {
	  if (here == null) {
		  return null;
	  }
	  final Operator op = JJNode.tree.getOperator(here);
	  if (op instanceof TypeDeclInterface) {
		  return here;
	  }
	  if (op instanceof Declaration) {
		  final IRNode parent = JJNode.tree.getParentOrNull(here);
		  if (ignoreNode((Declaration) op, parent)) {
			  return findClosestDecl(parent);
		  }
		  return here;
	  }
	  final IRNode parent = JJNode.tree.getParentOrNull(here);
	  return findClosestDecl(parent);
  }
  
  /**
   * Filter out cases that show up because node types are reused
   */
  private static boolean ignoreNode(Declaration d, IRNode parent) {
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
        for (IRNode typeParam : TypeFormals.getTypeIterator(types)) {
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
    case ANNOTATION:
      AnnotationBuilder ab = new AnnotationBuilder(name);
      ab.setVisibility(getVisibility(decl));
      return ab;
    case INTERFACE:
      InterfaceBuilder ib = new InterfaceBuilder(name);
      IRNode types = InterfaceDeclaration.getTypes(decl);
      int i = 0;
      for (IRNode typeParam : TypeFormals.getTypeIterator(types)) {
    	  TypeParameterBuilder tpb = buildTypeParameter(i, typeParam);
    	  ib.addTypeParameter(tpb);
    	  i++;
      }
      ib.setVisibility(getVisibility(decl));
      return ib;
    case TYPE_PARAMETER:
      final int num = computePosition(decl);
      // return buildTypeParameter(num, decl);
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
    for (IRNode bound : MoreBounds.getBoundIterator(bounds)) {
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
      for (IRNode param : Parameters.getFormalIterator(params)) {
        ParameterBuilder pb = buildParameter(param, i);
        c.addParameter(pb);
        i++;
      }
      types = ConstructorDeclaration.getTypes(decl);
      i = 0;
      for (IRNode typeParam : TypeFormals.getTypeIterator(types)) {
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
        for (IRNode param : Parameters.getFormalIterator(params)) {
          ParameterBuilder pb = buildParameter(param, i);
          m.addParameter(pb);
          i++;
        }
        types = MethodDeclaration.getTypes(decl);
        i = 0;
        for (IRNode typeParam : TypeFormals.getTypeIterator(types)) {
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
      // return buildParameter(decl, num, name);
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
