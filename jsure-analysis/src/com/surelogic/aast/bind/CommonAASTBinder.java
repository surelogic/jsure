/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/CommonAASTBinder.java,v 1.2 2008/10/28 21:28:51 dfsuther Exp $*/
package com.surelogic.aast.bind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.java.ArrayTypeNode;
import com.surelogic.aast.java.ClassTypeNode;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.MethodCallNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.PrimitiveTypeNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ReferenceTypeNode;
import com.surelogic.aast.java.SuperExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.TypeExpressionNode;
import com.surelogic.aast.java.VoidTypeNode;
import com.surelogic.aast.layers.AbstractLayerBinding;
import com.surelogic.aast.layers.LayerBindingKind;
import com.surelogic.aast.layers.TypeSetNode;
import com.surelogic.aast.layers.UnidentifiedTargetNode;
import com.surelogic.aast.promise.AnyInstanceExpressionNode;
import com.surelogic.aast.promise.EffectSpecificationNode;
import com.surelogic.aast.promise.ImplicitClassLockExpressionNode;
import com.surelogic.aast.promise.ImplicitQualifierNode;
import com.surelogic.aast.promise.ItselfNode;
import com.surelogic.aast.promise.QualifiedClassLockExpressionNode;
import com.surelogic.aast.promise.QualifiedLockNameNode;
import com.surelogic.aast.promise.QualifiedRegionNameNode;
import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.aast.promise.RegionNameNode;
import com.surelogic.aast.promise.SimpleLockNameNode;
import com.surelogic.aast.promise.ThreadRoleImportNode;
import com.surelogic.aast.promise.TypeDeclPatternNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.bind.FindLockModelStrategy;
import com.surelogic.annotation.bind.FindRegionModelStrategy;
import com.surelogic.annotation.rules.LayerRules;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.layers.IReferenceCheckDrop;
import com.surelogic.dropsea.ir.drops.layers.LayerPromiseDrop;
import com.surelogic.dropsea.ir.drops.layers.TypeSetPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.FindMethodStrategy;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaScope;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.UnversionedJavaBinder;
import edu.cmu.cs.fluid.java.bind.UnversionedJavaImportTable;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedDeclInterface;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class CommonAASTBinder extends AASTBinder {
  private final ITypeEnvironment tEnv;
  private final IBinder eb;

  public CommonAASTBinder(ITypeEnvironment te) {
    super(te.getBinder());
    tEnv = te;
    eb   = tEnv.getBinder();
  }
  
  /**
   * Wrapper introduced to check if we need to use a different ITypeEnvironment
   */
  IRNode findNamedType(String qname, IRNode context) { 
	  return tEnv.findNamedType(qname, context);
  }
  
  @Override
  public boolean isResolvable(FieldRefNode node) {
    ISourceRefType t = (ISourceRefType) node.getObject().resolveType();
    return (t == null) ? false : t.fieldExists(node.getId());
  }
  
  @Override
  public boolean isResolvable(ItselfNode node) {
	  return true; // It has to have a field decl to hang off of
  }
  
  @Override
  public ISourceRefType resolveType(ImplicitQualifierNode node) {
    final IRNode tdecl = findNearestType(node);
    return createISourceRefType(tdecl);
  }
  
  @Override
  public ISourceRefType resolveType(ThisExpressionNode node) {
    final IRNode tdecl = findNearestType(node);
    return createISourceRefType(tdecl);
  }
  
  @Override
  public ISourceRefType resolveType(SuperExpressionNode node) {
    final IRNode tdecl = findNearestType(node);
    return createISourceRefType(tdecl);
  }

  private IRegion findRegionModel(IJavaType jt, String name, IRNode context) {
    if (jt instanceof IJavaDeclaredType) {
      IJavaDeclaredType dt = (IJavaDeclaredType) jt;
      return findRegionModel(dt.getDeclaration(), name);
    }
    if (jt instanceof IJavaArrayType) {
      IRNode jlo = findNamedType(SLUtility.JAVA_LANG_OBJECT, context);
      return findRegionModel(jlo, name);
    }
    if (jt instanceof IJavaTypeFormal) {
      return findRegionModel(getDeclaredSuperclass(jt, tEnv), name, context);
    }
    if (jt instanceof IJavaPrimitiveType) {
      return null;
    }
    throw new IllegalArgumentException("Unexpected type: "+jt);
  }

  IJavaType getDeclaredSuperclass(IJavaType t, ITypeEnvironment tEnv) {
    IJavaType st = t.getSuperclass(tEnv);
    while (st instanceof IJavaTypeFormal) {
      st = st.getSuperclass(tEnv);
    }
    return st;
  }
  
  private IRegion findRegionModel(IType type, String name) {
    if (type == null) {
      return null;
    }
    return findRegionModel(type.getJavaType(), name, type.getNode());
  }
  
  private IRegion findRegionModel(IRNode tdecl, String name) {
    IRegion o = eb.findClassBodyMembers(tdecl, new FindRegionModelStrategy(eb, name), true);       
    return o;
  }
  
  @Override
  public boolean isResolvable(AASTNode node) {
	  return resolve(node) != null;
  }

  private IRNode resolveTypeName(final IRNode context, final String name) {
	IRNode t;
	if (name.indexOf('.') < 0) {
		t = findQualifiedType(context, name);  
	} else {
		t = findNamedType(name, context);    
		if (t == null) {
			// Try to find a package-qualified type
			t = findQualifiedType(context, name); 
		}
	}
    if (t != null && NamedPackageDeclaration.prototype.includes(t)) {
    	return null;
    }
    if (t == null) {
    	final IRNode cu = VisitUtil.findCompilationUnit(context);
    	final IJavaScope imports = UnversionedJavaImportTable.getImportTable(cu, (UnversionedJavaBinder) eb);
    	final LookupContext lookup = new LookupContext();
    	lookup.use(name, context);
    
    	edu.cmu.cs.fluid.java.bind.IBinding b = imports.lookup(lookup, IJavaScope.Util.isTypeDecl);
    	if (b != null) {
    		return b.getNode();
    	}
    	// Null otherwise
    }
    return t;
  }
  
  private IRNode findQualifiedType(final IRNode context, final String name) {
	int lastDot = name.lastIndexOf('.');
	IRNode t = null;
	if (lastDot < 0) {
		// Check if it's a type parameter for the method
		final Operator cop = JJNode.tree.getOperator(context);
		if (SomeFunctionDeclaration.prototype.includes(cop)) {
			IRNode typeParams = SomeFunctionDeclaration.getTypes(context);
			for(IRNode p : JJNode.tree.children(typeParams)) {
				if (name.equals(TypeFormal.getId(p))) {
					return p;
				}
			}
		}

		// Check if it's a local type
		for(IRNode type : VisitUtil.getEnclosingTypes(context, true)) {
			if (name.equals(JJNode.getInfoOrNull(type))) {
				return type;
			}
			// Check for nested types
			Operator op = JJNode.tree.getOperator(type);
			if (op instanceof NestedDeclInterface) {
				t = findNestedType(type, name);
				if (t != null) {
					return t;
				}
			}	
			// Check if it's a type parameter from the type
			IJavaDeclaredType dt = (IJavaDeclaredType) tEnv.convertNodeTypeToIJavaType(type);
			for(IJavaType param : dt.getTypeParameters()) {
				IJavaTypeFormal p = (IJavaTypeFormal) param;
				if (name.equals(TypeFormal.getId(p.getDeclaration()))) {
					return p.getDeclaration();
				}
			}
		}
		// Check if it's a top-level type in same package
		final String pkg = JavaNames.getPackageName(context);
		t = findNamedType(pkg+'.'+name, context);			
		if (t == null) {
			// Check if it's in the default package
			return findNamedType(name, context);
		}
		return t;
	} else {
		String prefix = name.substring(0, lastDot);
		t = resolveTypeName(context, prefix);
		/*
		if (t != null && t.identity() == IRNode.destroyedNode) {
			resolveTypeName(context, prefix); // For debugging
		}
		*/
		if (t != null && !NamedPackageDeclaration.prototype.includes(t)) {
			return findNestedType(t, name.substring(lastDot+1));
		}
	}
	return t;
  }

  private IRNode findNestedType(IRNode t, String name) {
	for(IRNode nt : VisitUtil.getNestedTypes(t)) {
		if (name.equals(JJNode.getInfoOrNull(nt))) {
			return nt;
		}
	}
	return null;
  }

  /*
  private static boolean nameMatches(String name, IRNode td) {	  
	  return name.equals(JavaNames.getFullTypeName(td)) ||
	         name.equals(JJNode.getInfoOrNull(td));
  }
  */
  
  private static String standardizeTypeName(NamedTypeNode t) {
      String name = t.getType();
      if (name == null || name.length() == 0) {
    	  return SLUtility.JAVA_LANG_OBJECT;
      }
      return name;
  }
  
  @Override
  public boolean isResolvableToType(AASTNode node) {
    if (node instanceof PrimitiveTypeNode) {
      return true;      
    }
    if (node instanceof NamedTypeNode) {
      NamedTypeNode t = (NamedTypeNode) node;
      final String name = standardizeTypeName(t);
      return "*".equals(name) || resolveTypeName(t.getPromisedFor(), name) != null;
    }
    else if (node instanceof ArrayTypeNode) {
    	ArrayTypeNode at = (ArrayTypeNode) node;
    	return isResolvableToType(at.getBase());
    }
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public IBinding resolve(AASTNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public IVariableBinding resolve(final ThisExpressionNode node) {
    // FIXME assumed to be on a method
	final IRNode mdecl = node.getPromisedFor();
	final IRNode receiver = JavaPromise.getReceiverNodeOrNull(mdecl);
	if (receiver == null) {
		return null;
	}
    return new IVariableBinding() {
      @Override
      public IJavaType getJavaType() {
        IRNode fast  = node.getPromisedFor();
        IRNode tdecl = VisitUtil.getClosestType(fast);
        return JavaTypeFactory.getMyThisType(tdecl);
      }
      @Override
      public IRNode getNode() {
    	return receiver;
      }
    };
  }
  
  @Override
  public IVariableBinding resolve(final SuperExpressionNode node) {
    // FIXME assumed to be on a method
    return new IVariableBinding() {
      @Override
      public IJavaType getJavaType() {
        IRNode fast  = node.getPromisedFor();
        IRNode tdecl = VisitUtil.getClosestType(fast);
        return JavaTypeFactory.getMyThisType(tdecl);
      }
      @Override
      public IRNode getNode() {
        IRNode mdecl = node.getPromisedFor();
        return JavaPromise.getReceiverNodeOrNull(mdecl);
      }
    };
  }
  
  @Override
  public IVariableBinding resolve(final QualifiedThisExpressionNode node) {
    final ISourceRefType type = node.getType().resolveType();
    if (type == null) {
      return null;
    }
    // Actually, this could be a method or a type
    final IRNode decl;
    IRNode temp = VisitUtil.getClosestClassBodyDecl(node.getPromisedFor());
    if (FieldDeclaration.prototype.includes(temp)) {
    	decl = VisitUtil.getEnclosingType(temp);
    } else {
    	decl = temp;
    }
    try {
    	final IRNode rv = JavaPromise.getQualifiedReceiverNodeByName(decl, type.getNode());
    	if (rv == null) {
    		JavaPromise.getQualifiedReceiverNodeByName(decl, type.getNode());
    		return null;
    		/*
    } else {
    	// Check for "other" refs to enclosing types
    	IRNode tdecl = VisitUtil.getClosestType(decl);
    	IRNode enclosingT = VisitUtil.getEnclosingType(tdecl);
    	if (type.getNode() != enclosingT && type.getNode() != tdecl) {
    		// TODO how to explain why this doesn't "bind"?
    		return null;
    	}
    		 */
    	}
    	return new IVariableBinding() {
    		@Override
    		public IJavaType getJavaType() {
    			return type.getJavaType();
    		}
    		@Override
    		public IRNode getNode() {  
    			return rv;
    		}
    	};
    } catch (Exception e) {
    	e.printStackTrace(System.out);
    	return null;
    }
  }

  @Override
  public IVariableBinding resolve(FieldRefNode node) {
    ISourceRefType t = (ISourceRefType) node.getObject().resolveType();
    return t.findField(node.getId());
  }
  
  @Override
  public IVariableBinding resolve(final ItselfNode node) {
	  return new IVariableBinding() {		
		@Override
    public IRNode getNode() {
			return node.getPromisedFor();
		}
		
		@Override
    public IJavaType getJavaType() {
			IRNode n = getNode();
			return eb.getJavaType(n);
		}
	};
  }
  
  @Override
  public IRegionBinding resolve(RegionNameNode node) {
    if (node.getParent() instanceof EffectSpecificationNode) {
      EffectSpecificationNode effect = (EffectSpecificationNode) node.getParent();
      IType t                        = effect.getContext().resolveType();
      if (t == null) {
    	  effect.getContext().resolveType();
      }
      return findRegionModel(t, node.getId());
    }
    
    // Need to handle from/to differently for region mappings
    if (node.getParent() instanceof RegionMappingNode) {
      RegionMappingNode mapping = (RegionMappingNode) node.getParent();
      if (node.equals(mapping.getFrom())) {
        IRNode vdecl = mapping.getPromisedFor();
        IRNode type  = VariableDeclarator.getType(vdecl);
        IJavaType jt = eb.getJavaType(type);
        return findRegionModel(jt, node.getId(), vdecl);
      }
    }
    final IRNode tdecl = findNearestType(node);
    return findRegionModel(tdecl, node.getId());
  }
  
  @Override
  public IRegionBinding resolve(QualifiedRegionNameNode node) {
    final ISourceRefType type = node.getType().resolveType();
    return findRegionModel(type, node.getId());
  }

  private LockModel findLockModel(IRNode tdecl, String name) {
	if (tdecl == null) {
		return null;
	}
    LockModel o = eb.findClassBodyMembers(tdecl, new FindLockModelStrategy(eb, name), true);       
    return o;
  }
  
  @Override
  public ILockBinding resolve(SimpleLockNameNode node) {
    final IRNode tdecl = findNearestType(node);
    return findLockModel(tdecl, node.getId());
  }
  
  @Override
  public ILockBinding resolve(QualifiedLockNameNode node) {
    final IType type = node.getBase().resolveType();
    if (type == null) {
      return null;
    }
    return findLockModel(type.getNode(), node.getId());
  }
  
  /* (non-Javadoc)
   * @see com.surelogic.aast.bind.IAASTBinder#resolve(com.surelogic.aast.promise.ColorImportNode)
   */
  @Override
  public IRNode resolve(ThreadRoleImportNode node) {
    // the ColorImportNode has an id String (accessible via getID()) that is either
    // the name of a Type or the name of a package.  When it's a type, we want to get
    // back the IRNode that is the foo_Decl of the type; for a package, we want the 
    // IRNode that is the package node of the package-info.java file for that package
    // (or, equivalently, the package node that represents the package-name.promises.xml
    // file for that package.
    final String importedName = node.getId();
    // try getting a package of that name
    IRNode res = tEnv.findPackage(importedName, node.getPromisedFor());
    if (res == null) {
      res = findNamedType(importedName, node.getPromisedFor());
    }
   return res;
  }
  
  @Override
  public IType resolveType(AASTNode node) {
    if (node instanceof IHasVariableBinding) {
      IHasVariableBinding ivb = (IHasVariableBinding) node;
      IVariableBinding vb     = ivb.resolveBinding();
      if (vb == null) {
    	  ivb.resolveBinding();
      }
      return createIType(vb);      
    }
    /*
    if (node instanceof VariableUseExpressionNode) {
      VariableUseExpressionNode v = (VariableUseExpressionNode) node;
      return createIType(v.resolveBinding());
    }
    else if (node instanceof FieldRefNode) {
      FieldRefNode ref = (FieldRefNode) node;
      return createIType(ref.resolveBinding());
    }
    */
    if (node instanceof ImplicitQualifierNode) {
      final IRNode type = findNearestType(node);
      return createIType(type);
    }
    if (node instanceof ImplicitClassLockExpressionNode) {
      final IRNode type = findNearestType(node);
      return createIType(type);
    }
    if (node instanceof QualifiedClassLockExpressionNode) {
      QualifiedClassLockExpressionNode lock = (QualifiedClassLockExpressionNode) node;
      return lock.getType().resolveType();
    }
    if (node instanceof TypeExpressionNode) {
      TypeExpressionNode t = (TypeExpressionNode) node;
      return t.getType().resolveType();
    }
    if (node instanceof AnyInstanceExpressionNode) { 
      AnyInstanceExpressionNode aie = (AnyInstanceExpressionNode) node;
      return aie.getType().resolveType();
    }
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  private IType createIType(final IRNode type) {
    if (type == null) {
      return null;
    }
    return new Type(type);
  }
  
  @Override
  protected ISourceRefType createISourceRefType(final IRNode type) {
    if (type == null) {
      return null;
    }
    return new SourceRefType(type, eb);
  }

  private IType createIType(final IVariableBinding vb) {
    if (vb != null) {
      IJavaType t = vb.getJavaType();
      if (t instanceof IJavaDeclaredType) {
        IJavaDeclaredType dt = (IJavaDeclaredType) t;
        return new SourceRefType(dt.getDeclaration(), eb); 
      }
      
      IRNode n    = vb.getNode();
      Operator op = JJNode.tree.getOperator(n);
      final IRNode type;
      if (ParameterDeclaration.prototype.includes(op)) {
        type = ParameterDeclaration.getType(n);
      } else if (QualifiedReceiverDeclaration.prototype.includes(op)) {
        type = QualifiedReceiverDeclaration.getBase(n);
      } else if (ReceiverDeclaration.prototype.includes(op)) {
        type = VisitUtil.getClosestType(n);
        return new SourceRefType(type, eb); 
      } else {
        type = VariableDeclarator.getType(n);
      }
      /*
      if (JJNode.tree.getOperator(type) instanceof ArrayType) {
    	  System.out.println("Got array type: "+DebugUnparser.toString(type));
      }
      */
      return new IType() {
        @Override
        public IRNode getNode() {
          return eb.getBinding(type);
        }
        @Override
        public IJavaType getJavaType() {
          return eb.getJavaType(type);
        }
      };
    }
    return null;
  }

  @Override
  public IReferenceType resolveType(ReferenceTypeNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public ISourceRefType resolveType(ClassTypeNode node) {
    if (node instanceof NamedTypeNode) {
      NamedTypeNode t = (NamedTypeNode) node;
      final String name = standardizeTypeName(t);
      IRNode td = resolveTypeName(t.getPromisedFor(), name);
      if (td == null) {
    	  return null;
      }
      return createISourceRefType(td);
    }
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public IPrimitiveType resolveType(PrimitiveTypeNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public IVoidType resolveType(VoidTypeNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public Iterable<ISourceRefType> resolveType(TypeDeclPatternNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  @Override
  public IMethodBinding resolve(MethodCallNode node) {
	final ExpressionNode e = node.getObject();
	final IType t = e.resolveType();
	if (t == null) {
		return null;
	}
    final IRNode fast = t.getNode();
    final IRNode tdecl = VisitUtil.getClosestType(fast);
    @SuppressWarnings("unchecked")
    final IRNode method = (IRNode) eb.findClassBodyMembers(tdecl,
        new FindMethodStrategy(binder, node.getId(), JavaGlobals.noTypes),
        false);
    if (method != null) {
      return new IMethodBinding() {
        @Override
        public IRNode getNode() {
          return method;
        }
      };
    }
    return null;
  }

  @Override
  public boolean isResolvable(MethodCallNode node) {
	  return resolve(node) != null;
  }

  @Override
  public boolean isResolvable(UnidentifiedTargetNode node) {
	  return resolve(node) != null;
  }

  @Override
  public ILayerBinding resolve(UnidentifiedTargetNode node) {
	  final AASTNode parent = node.getParent();
	  final AASTRootNode root = parent.getRoot();
	  final String name = node.getQualifiedName();
	  
	  ILayerBinding rv = null;
	  // layer, typeset, pkg, or type
	  if (!(root instanceof TypeSetNode)) {
		  rv = findLayer(root.getPromisedFor(), name);
	  }	  
	  if (rv == null) {
		  rv = findTypeSet(root.getPromisedFor(), name);
	  }
	  if (rv == null) {
		  rv = findPackageOrType(root.getPromisedFor(), name);
	  } 
	  return rv;
  }

  private ILayerBinding findLayer(IRNode context, String qname) {
	  final String pkg, name;
	  final int lastDot = qname.lastIndexOf('.');
	  if (lastDot < 0) {
		  // unqualified name
		  pkg = VisitUtil.getPackageName(VisitUtil.findRoot(context));
		  name = qname;
	  } else {
		  name = qname.substring(lastDot+1);
		  pkg = qname.substring(0, lastDot);
	  }
	  final IRNode pkgNode = tEnv.findPackage(pkg, context);
	  final LayerPromiseDrop d = LayerRules.findLayer(pkgNode, name);
	  if (d != null) {
		  return new AbstractLayerBinding(LayerBindingKind.LAYER) {
			  @Override public IReferenceCheckDrop getOther() {
				  return d;
			  }
		  };
	  }
	  return null;
  }

  private ILayerBinding findTypeSet(IRNode context, String qname) {
	  final String pkg, name;
	  final int lastDot = qname.lastIndexOf('.');
	  if (lastDot < 0) {
		  // unqualified name
		  pkg = VisitUtil.getPackageName(VisitUtil.findRoot(context));
		  name = qname;
	  } else {
		  name = qname.substring(lastDot+1);
		  pkg = qname.substring(0, lastDot);
	  }
	  final IRNode pkgNode = tEnv.findPackage(pkg, context);
	  final TypeSetPromiseDrop d = LayerRules.findTypeSet(pkgNode, name);
	  if (d != null) {
		  return new AbstractLayerBinding(LayerBindingKind.TYPESET) {
			  @Override public IReferenceCheckDrop getOther() {
				  return d;
			  }
		  };
	  }
	  return null;
  }
  
  private ILayerBinding findPackageOrType(IRNode context, String qname) {
	  if (qname.endsWith("+")) {
		  final String prefix = qname.substring(0, qname.length()-1);
		  final List<IRNode> pkgs = new ArrayList<IRNode>();
		  
		  final IIRProject proj = Projects.getEnclosingProject(context);
		  for(Pair<String,IRNode> p : proj.getTypeEnv().getPackages()) {
			  if (p.first().startsWith(prefix)) {
				  IRNode pkg = p.second();
				  if (pkg.identity() != IRNode.destroyedNode) {
					  pkgs.add(pkg);
				  }
			  }
		  }
		  if (!pkgs.isEmpty()) {
			  return new AbstractLayerBinding(LayerBindingKind.PACKAGE) {
				  @Override public Iterator<IRNode> iterator() {
					  return pkgs.iterator();
				  }
			  };
		  }
		  //return null;
		  return emptyPackageB;
	  }
	  //final IRNode t = findNamedType(qname, context);
	  final IRNode t = resolveTypeName(context, qname);
	  if (t != null && TypeDeclaration.prototype.includes(t)) {
		  return new AbstractLayerBinding(LayerBindingKind.TYPE) {
			  @Override public IRNode getType() {
				  return t;
			  }
		  }; 
	  }
	  final IRNode p = tEnv.findPackage(qname, context);
	  if (p != null) {
		  return new AbstractLayerBinding(LayerBindingKind.PACKAGE) {
			  @Override public Iterator<IRNode> iterator() {
				  return new SingletonIterator<IRNode>(p);
			  }
		  };
	  }
	  int lastDot = qname.lastIndexOf('.');
	  String name;
	  if (lastDot < 0) {
		  name = qname;
	  } else {
		  name = qname.substring(lastDot+1);
	  }
	  // TODO Hack since we don't know if the name exists
	  if (name.length() == 0) {
		  return null;
	  }
	  if (Character.isUpperCase(name.charAt(0))) {
		  return new AbstractLayerBinding(LayerBindingKind.TYPE) {
			  @Override public IRNode getType() {
				  return null;
			  }
		  };
	  } else {
		  return emptyPackageB;
	  }
	  //return null;  
  }
  
  private static ILayerBinding emptyPackageB = new AbstractLayerBinding(LayerBindingKind.PACKAGE) {
	  @Override public Iterator<IRNode> iterator() {
		  return Collections.<IRNode>emptyList().iterator();
	  }
  };
}
