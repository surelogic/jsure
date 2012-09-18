// $Header: /var/cvs/fluid/code/fluid/java/bind/CogenUtil.java,v 1.6 2002/08/06 02:14:07 chance Exp $
package edu.cmu.cs.fluid.java.util;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.parse.AstGen;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class CogenUtil implements JavaGlobals {
  private static final Logger LOG = SLLogger.getLogger("FLUID.java");
  
  /// makeNodeArray
  public static IRNode[] makeNodeArray(Vector<IRNode> v) {
    // TODO: Why not?
    // return (IRNode[])v.toArray(noNodes);
    final int size   = v.size();
    final IRNode[] a = (size == 0) ? noNodes : new IRNode[size];
    for(int i=0; i<a.length; i++) {
      a[i] = v.elementAt(i);
    }
    return a;
  }

  /// copyProperties (info, mods)
  public static void copyProperties(IRNode src, IRNode dest) {
    JJNode.setInfo(src, JJNode.getInfo(dest));
    JavaNode.setModifiers(src, JavaNode.getModifiers(dest));
  }

  public static IRNode createEmptyAnnos() {
    return Annotations.createNode(noNodes);
  }
  
  /// createImplicitBase 
  public static IRNode createImplicitBase(IRNode here) {
    IRNode cbDecl = OpSearch.memberSearch.findEnclosing(here);
    if (JavaNode.getModifier(cbDecl, JavaNode.STATIC)) {
      IRNode type = OpSearch.typeSearch.findEnclosing(cbDecl);
      return createTypeE(JJNode.getInfo(type));
    } else {
      // System.out.println("This2: "+DebugUnparser.toString(cbDecl));
      return ThisExpression.prototype.jjtCreate();
    }
  }

  public static IRNode createQualifiedImplicitBase(IRNode type, IRNode here) {
    boolean isInstance = true;
    if (isInstance) {
      return QualifiedThisExpression.createNode(type);
    } else {
      return type;
    }
  }

  /// createNamedT
  public static IRNode createNamedT(String n) {
    return NamedType.createNode(CommonStrings.intern(n));
  }

  public static IRNode createType(ITypeEnvironment tEnv, IJavaType t) {
	if (t instanceof IJavaPrimitiveType) {		
		IJavaPrimitiveType pt = (IJavaPrimitiveType) t;
		return pt.getOp().jjtCreate();
	}
	else if (t instanceof IJavaArrayType) {
		IJavaArrayType at = (IJavaArrayType) t;
		IRNode base       = createType(tEnv, at.getBaseType());
		return ArrayType.createNode(base, at.getDimensions());
	}
	else if (t instanceof IJavaDeclaredType) {
		IJavaDeclaredType dt = (IJavaDeclaredType) t;
		IRNode base          = createNamedType(dt.getDeclaration());
		if (dt.getTypeParameters() == null || dt.getTypeParameters().isEmpty()) {
			return base;
		}		
		IRNode[] params = adaptTypes(tEnv, dt.getTypeParameters());
		return ParameterizedType.createNode(base, TypeActuals.createNode(params));		
	}
	else if (t instanceof IJavaTypeFormal) {		
		IJavaTypeFormal tf = (IJavaTypeFormal) t;
		/*
		IRNode[] bounds    = adaptTypes(tEnv, tf.getSupertypes(tEnv));
		return TypeFormal.createNode(tf.getName(), MoreBounds.createNode(bounds));
		*/
		IRNode formal = tf.getDeclaration();
		return NamedType.createNode(TypeFormal.getId(formal));
	}
	else if (t instanceof IJavaWildcardType) {
		IJavaWildcardType wt = (IJavaWildcardType) t;
		if (wt.getLowerBound() != null) {
			IRNode lower = createType(tEnv, wt.getLowerBound());
			return WildcardExtendsType.createNode(lower);
		}
		if (wt.getUpperBound() != null) {
			IRNode upper = createType(tEnv, wt.getUpperBound());
			return WildcardSuperType.createNode(upper);
		}
		return WildcardType.prototype.jjtCreate();
	}
	else if (t instanceof IJavaCaptureType) {		
		IJavaCaptureType ct = (IJavaCaptureType) t;
		/*
		IRNode lower = createType(tEnv, ct.getLowerBound());
		IRNode upper = createType(tEnv, ct.getLowerBound());
		return CaptureType.createNode(lower, upper);
		*/
		return createType(tEnv, ct.getWildcard());
	}
	else if (t instanceof  IJavaNullType) {
		return NamedType.createNode("java.lang.Object");
	}
	throw new UnsupportedOperationException("Unsupported type: "+t);
  }
  
  private static IRNode[] adaptTypes(ITypeEnvironment tEnv, Iterable<IJavaType> types) {
	  List<IRNode> nodes = new ArrayList<IRNode>();
	  for(IJavaType t : types) {
		  nodes.add(createType(tEnv, t));
	  }
	  return nodes.toArray(noNodes);
  }
  
  public static IRNode createNamedType(IRNode tdecl) {
    Operator op = jtree.getOperator(tdecl);
    if (op instanceof AnonClassExpression) {
    	/*
    	// Use base type
    	IRNode base = AnonClassExpression.getType(tdecl);
    	if (ParameterizedType.prototype.includes(base)) {
    		base = ParameterizedType.getBase(base);
    	}
    	return JJNode.copyTree(base);
    	*/
    	// Use manufactured name
    	String name = JJNode.getInfoOrNull(tdecl);
    	return NamedType.createNode(name);
    }
    String name = JJNode.getInfo(tdecl);
    if (op instanceof NestedTypeDeclInterface ||
        op instanceof NestedEnumDeclaration || op instanceof NestedAnnotationDeclaration) {
      // Check if a local class
      IRNode enclosing = VisitUtil.getEnclosingClassBodyDecl(tdecl);
      if (enclosing != null && SomeFunctionDeclaration.prototype.includes(enclosing)) {
        //System.out.println("Converting type within a function");
        return NamedType.createNode(name); 
      }
      return TypeRef.createNode(createNamedType(VisitUtil.getEnclosingType(tdecl)),name);
    }
    if (TypeUtil.isOuter(tdecl)) {
      String qname = TypeUtil.getQualifiedName(tdecl);
   	  qname = CommonStrings.intern(qname);
      return NamedType.createNode(qname);
    }
    name = CommonStrings.intern(name);
    return NamedType.createNode(name);
  }
  
  /// createTypeE
  public static IRNode createTypeE(String n) {
    IRNode t = NamedType.createNode(CommonStrings.intern(n));
    return TypeExpression.createNode(t);
  }

  /// makeFoo
  public static IRNode makeObjectNamedT() {
    return NamedType.createNode("java.lang.Object");
  }
  
  public static IRNode makeType(IJavaType ty) {
    if (ty instanceof IJavaReferenceType) {
      if (ty instanceof IJavaDeclaredType) {
        IJavaDeclaredType dt = (IJavaDeclaredType)ty;
        IRNode tdecl = dt.getDeclaration();
        IRNode result;
        if (dt instanceof IJavaNestedType) {
          IJavaNestedType nt = (IJavaNestedType)dt;
          result = TypeRef.createNode(makeType(nt.getOuterType()),JJNode.getInfo(tdecl));
        } else {
          result = createNamedType(tdecl);
        }
        List<IJavaType> typeParameters = dt.getTypeParameters();
        int parms = typeParameters.size();
        if (parms > 0) {
          IRNode[] targs = new IRNode[parms];
          for (int i=0; i < parms; ++i) {
            targs[i] = makeType(typeParameters.get(i));
          }
          result = ParameterizedType.createNode(result,TypeActuals.createNode(targs));
        }
        return result;
      } else if (ty instanceof IJavaArrayType) {
        IJavaArrayType at = (IJavaArrayType)ty;
        return ArrayType.createNode(makeType(at.getBaseType()),at.getDimensions());
      } else if (ty instanceof IJavaNullType) {
        LOG.warning("Asked to create a null type");
        return makeObjectNamedT();
      }
    } else if (ty instanceof IJavaPrimitiveType) {
      return JavaNode.makeJavaNode(((IJavaPrimitiveType)ty).getOp());
    } 
    LOG.warning("Unknown type: " + ty);
    return null;
  }

  /// makeVarDecl
  public static IRNode makeVarDecl(String name, IRNode initE) {
    IRNode init = (initE == null) ?
      NoInitialization.prototype.jjtCreate() :
      Initialization.createNode(initE);
    return VariableDeclarator.createNode(name, 0, init);
  }

  /// makeVarDecls
  public static IRNode makeVarDecls(String name, IRNode initE) {
    IRNode[] vD = new IRNode[]{ makeVarDecl(name, initE) };
    return VariableDeclarators.createNode(vD);
  }

  public static IRNode makeVarDecls(IRNode vdecl) {
    return VariableDeclarators.createNode((vdecl == null) ?
					  noNodes : new IRNode[] { vdecl });
  }

  /// makeDeclStmt
  public static IRNode makeDeclStmt(IRNode[] annos, int mods, String name, IRNode type,
				     IRNode initE) {
    return DeclStatement.createNode(Annotations.createNode(annos), mods, type, makeVarDecls(name, initE));
  }

  /// makeFieldDecl
  public static IRNode makeFieldDecl(IRNode[] annos, int mods, String name, IRNode type,
				     IRNode initE) {
    return FieldDeclaration.createNode(Annotations.createNode(annos), mods, type, makeVarDecls(name, initE));
  }
  
  public static IRNode makeFieldDecl(IRNode[] annos, int mods, IRNode type, IRNode decl) {
  	return FieldDeclaration.createNode(Annotations.createNode(annos), mods, type, makeVarDecls(decl));
  }

  /// makeParamDecl
  public static IRNode makeParamDecl(IRNode[] annos, int mods, String name, IRNode type) {
    return ParameterDeclaration.createNode(Annotations.createNode(annos), mods, type, name);
  }
  public static IRNode makeParamDecl(String name, IRNode type) {
    return makeParamDecl(noNodes, JavaNode.ALL_FALSE, name, type); 
  }

  /// makeMethodBody
  public static IRNode makeMethodBody(IRNode[] stmts) {
    return MethodBody.createNode(BlockStatement.createNode(stmts));
  }

  /// makeMethodDecl
  public static IRNode makeMethodDecl(IRNode[] annos, int mods, IRNode[] formals,
              IRNode type, String name,
				      IRNode[] params, IRNode[] throwC, 
				      IRNode body) {
    // System.out.println(DebugUnparser.toString(type));
    body = (body != null) ? body : NoMethodBody.prototype.jjtCreate();
    return MethodDeclaration.createNode(Annotations.createNode(annos), mods, 
          TypeFormals.createNode(formals), 
          type, name, 
					Parameters.createNode(params), 0,
					Throws.createNode(throwC), body);
  }

  /// makeConstructorDecl
  public static IRNode makeConstructorDecl(IRNode[] annos, int mods, IRNode[] formals, String name,
					   IRNode[] params, IRNode[] throwC, 
					   IRNode body) {
    body = (body != null) ? body : CompiledMethodBody.prototype.jjtCreate();
    return ConstructorDeclaration.createNode(Annotations.createNode(annos), mods, 
               TypeFormals.createNode(formals), 
               name, 
					     Parameters.createNode(params),
					     Throws.createNode(throwC), body);
  }

  /// makeAnonClass
  public static JavaNode makeAnonClass(IRNode type, IRNode[] args, 
				     IRNode[] members) {
    return AnonClassExpression.createNode(
        NonPolymorphicNewExpression.createNode(type, Arguments.createNode(args)),
					  ClassBody.createNode(members));
  }

  /// makeClass
  public static IRNode makeClass(boolean nested, int mods, String name, 
         IRNode[] formals, 
				 IRNode type, IRNode[] imps, IRNode[] members){
    JavaOperator op = nested ? 
      NestedClassDeclaration.prototype : ClassDeclaration.prototype;
    
    IRNode t = op.jjtCreate();
    jtree.setChild(t, ClassDeclaration.annosLoc, Annotations.createNode(noNodes));
    jtree.setChild(t, ClassDeclaration.typesLoc, TypeFormals.createNode(formals));
    jtree.setChild(t, ClassDeclaration.extensionLoc, type);
    jtree.setChild(t, ClassDeclaration.implsLoc, Implements.createNode(imps));
    finishTypeInit(t, mods, name, members, ClassDeclaration.bodyLoc);
    return t;
  }

  /// makeInterface
  public static IRNode makeInterface(boolean nested, int mods, String name, 
             IRNode[] formals,
				     IRNode[] imps, IRNode[] members) {
    JavaOperator op = nested ? 
      NestedInterfaceDeclaration.prototype : InterfaceDeclaration.prototype;

    IRNode t = op.jjtCreate();
    jtree.setChild(t, InterfaceDeclaration.annosLoc, Annotations.createNode(noNodes));
    jtree.setChild(t, InterfaceDeclaration.typesLoc, TypeFormals.createNode(formals));
    jtree.setChild(t, InterfaceDeclaration.extensionsLoc, Extensions.createNode(imps));
    finishTypeInit(t, mods, name, members, InterfaceDeclaration.bodyLoc);
    return t;
  }

  /// finishTypeInit 
  private static void finishTypeInit(IRNode t, int mods, String name, 
				     IRNode[] members, int memNum) { 
    JavaNode.setModifiers(t, mods);
    JJNode.setInfo(t, name);
    jtree.setChild(t, memNum, ClassBody.createNode(members));
  }

  /// makeEmpty{Type}
  public static IRNode makeNewClass(boolean nested, int mods, String name) {
    return makeClass(nested, mods, name, noNodes,
		     createNamedT("java.lang.Object"), noNodes, noNodes);
  }
  public static IRNode makeNewInterface(boolean nested, int mods, String name){
    return makeInterface(nested, mods, name, noNodes, noNodes, noNodes);
  }
  // static IRNode makeEmptyAnonClass(IRNode constructor) {}
  public static IRNode makeNewAnonClass(String name) {
    // return makeAnonClass(createNamedT(name), noNodes, noNodes);
    return AstGen.genExpr("new "+name+"() {}");
  }

  /// makeSimpleCU -- no imports
  public static IRNode makeSimpleCU(String pkg, IRNode type) {
    IRNode[] types = new IRNode[] { type };
    return CompilationUnit.createNode(NamedPackageDeclaration.createNode(createEmptyAnnos(), pkg),
				      ImportDeclarations.createNode(noNodes),
				      TypeDeclarations.createNode(types));
  }

  /// makeDefaultConstructorCall
  public static IRNode makeDefaultConstructorCall() {
    //! TODO: COnstructorCall no longer a Statement!
    /* return ConstructorCall.createNode(ThisExpression.prototype.jjtCreate(),
       Arguments.createNode(noNodes));
    */
    return AstGen.genConstructorCall("this();");
  }
  /// makeDefaultSuperCall
  /**
   * Create a non-polymorphic call to super.
   * @return the statement with the call node.
   * @see #makeDefaultConstructorCall()
   */
  public static IRNode makeDefaultSuperCall() {
    //! TODO COnstructorCall no longer a statement
    // return AstGen.genConstructorCall("super();");
    IRNode call = NonPolymorphicConstructorCall.createNode(
                                             SuperExpression.prototype.jjtCreate(), 
                                             Arguments.createNode(noNodes));
    call = ExprStatement.createNode(call);
    return call;                              
  }

  /// makeBlock
  public static IRNode makeBlock(IRNode stmt) {
    return BlockStatement.createNode((stmt == null) ? 
				     noNodes : new IRNode[] { stmt });
  }
  
  /// makeLvDef
  public static IRNode makeLvDef(String name, IRNode expr) {
    IRNode assign = AssignExpression.createNode(makeUseE(name), expr);
    return ExprStatement.createNode(assign);
  }
  
  /// makeUseE
  public static IRNode makeUseE(String name) {
    return VariableUseExpression.createNode(name);
  }

  /// other stuff
  public static IRNode makeCorrespondingTypeRef(IRNode type) {

    Operator op = JJNode.tree.getOperator(type);
    if (op instanceof NestedTypeDeclInterface ||
        op instanceof AnonClassExpression) {
    	return makeTypeRefForEnclosedType(type, null);
    }
	// Handle method-local classes?
    IRNode enclosingT = VisitUtil.getEnclosingType(type);
    if (enclosingT != null) {
    	return makeTypeRefForEnclosedType(type, enclosingT);
    }
    String name = JavaNames.getFullTypeName(type);
    name = CommonStrings.intern(name);
    return NamedType.createNode(name);
  }
  
  private static IRNode makeTypeRefForEnclosedType(IRNode type, IRNode enclosingT) {
	  if (enclosingT == null) {
		  enclosingT = VisitUtil.getEnclosingType(type);
	  }
      IRNode tr         = makeCorrespondingTypeRef(enclosingT);
      String name       = JavaNames.getTypeName(type);
      return TypeRef.createNode(tr, name);
  }
}
