/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/CommonAASTBinder.java,v 1.2 2008/10/28 21:28:51 dfsuther Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IBinding;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.bind.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class CommonAASTBinder extends AASTBinder {
  private final ITypeEnvironment tEnv;
  private final IBinder eb;

  public CommonAASTBinder(ITypeEnvironment te) {
    super(te.getBinder());
    tEnv = te;
    eb   = tEnv.getBinder();
  }
  
  public boolean isResolvable(FieldRefNode node) {
    ISourceRefType t = (ISourceRefType) node.getObject().resolveType();
    return (t == null) ? false : t.fieldExists(node.getId());
  }
  
  public ISourceRefType resolveType(ThisExpressionNode node) {
    final IRNode tdecl = findNearestType(node);
    return createISourceRefType(tdecl);
  }
  
  public ISourceRefType resolveType(SuperExpressionNode node) {
    final IRNode tdecl = findNearestType(node);
    return createISourceRefType(tdecl);
  }

  private IRegion findRegionModel(IJavaType jt, String name) {
    if (jt instanceof IJavaDeclaredType) {
      IJavaDeclaredType dt = (IJavaDeclaredType) jt;
      return findRegionModel(dt.getDeclaration(), name);
    }
    if (jt instanceof IJavaArrayType) {
      if (PromiseConstants.REGION_ELEMENT_NAME.equals(name)) {
        return RegionModel.getInstance(PromiseConstants.REGION_ELEMENT_NAME);
      }
      if (PromiseConstants.REGION_LENGTH_NAME.equals(name)) {
    	return RegionModel.getInstance(PromiseConstants.REGION_LENGTH_NAME);
      }
      IRNode jlo = tEnv.findNamedType("java.lang.Object");
      return findRegionModel(jlo, name);
    }
    if (jt instanceof IJavaTypeFormal) {
      return findRegionModel(getDeclaredSuperclass(jt, tEnv), name);
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
    return findRegionModel(type.getJavaType(), name);
  }
  
  private IRegion findRegionModel(IRNode tdecl, String name) {
    IRegion o = eb.findClassBodyMembers(tdecl, new FindRegionModelStrategy(eb, name), true);       
    return o;
  }
  
  public boolean isResolvable(AASTNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  private IRNode resolveTypeName(final AASTNode a, final String name) {
    IRNode t = tEnv.findNamedType(name);
    if (t == null) {
      IRNode context = a.getPromisedFor();
      // Check if it's a top-level type
      String pkg     = JavaNames.getPackageName(context);
      t = tEnv.findNamedType(pkg+'.'+name);
      
      if (t == null) {
    	boolean prevWasNested = false;
       loop:
    	// Check enclosing types
    	for(IRNode td : VisitUtil.getEnclosingTypes(context)) {
    	  if (name.equals(JJNode.getInfoOrNull(td))) {
    	    t = td;
    	    break loop;
    	  }
    	  // Check for member types
    	  if (prevWasNested) {
    		prevWasNested = false;
    		for(IRNode member : VisitUtil.getClassBodyMembers(td)) {
    	      Operator mop = JJNode.tree.getOperator(member);
    	      if (mop instanceof NestedDeclInterface && 
    	          name.equals(JJNode.getInfoOrNull(member))) {
    	    	t = td;
    	    	break loop;
    	      }
    		}
    	  }
    	  Operator op = JJNode.tree.getOperator(td);    	  
    	  if (op instanceof NestedDeclInterface) {    		
    		prevWasNested = true;
    	  }
    	}
      }
    }
    return t;
  }
  
  public boolean isResolvableToType(AASTNode node) {
    if (node instanceof PrimitiveTypeNode) {
      return true;      
    }
    if (node instanceof NamedTypeNode) {
      NamedTypeNode t = (NamedTypeNode) node;
      return resolveTypeName(t, t.getType()) != null;
    }
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  public IBinding resolve(AASTNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  public IVariableBinding resolve(final ThisExpressionNode node) {
    // FIXME assumed to be on a method
    return new IVariableBinding() {
      public IJavaType getJavaType() {
        IRNode fast  = node.getPromisedFor();
        IRNode tdecl = VisitUtil.getClosestType(fast);
        return JavaTypeFactory.getMyThisType(tdecl);
      }
      public IRNode getNode() {
        IRNode mdecl = node.getPromisedFor();
        return JavaPromise.getReceiverNodeOrNull(mdecl);
      }
    };
  }
  
  public IVariableBinding resolve(final SuperExpressionNode node) {
    // FIXME assumed to be on a method
    return new IVariableBinding() {
      public IJavaType getJavaType() {
        IRNode fast  = node.getPromisedFor();
        IRNode tdecl = VisitUtil.getClosestType(fast);
        return JavaTypeFactory.getMyThisType(tdecl);
      }
      public IRNode getNode() {
        IRNode mdecl = node.getPromisedFor();
        return JavaPromise.getReceiverNodeOrNull(mdecl);
      }
    };
  }
  
  public IVariableBinding resolve(final QualifiedThisExpressionNode node) {
    final ISourceRefType type = node.getType().resolveType();
    if (type == null) {
      return null;
    }
    return new IVariableBinding() {
      public IJavaType getJavaType() {
        return type.getJavaType();
      }
      public IRNode getNode() {
        IRNode mdecl = VisitUtil.getClosestClassBodyDecl(node.getPromisedFor());
        IRNode rv    = JavaPromise.getQualifiedReceiverNodeByName(mdecl, type.getNode());
        if (rv == null) {
          JavaPromise.getQualifiedReceiverNodeByName(mdecl, type.getNode());
        }
        return rv;
      }
    };
  }

  public IVariableBinding resolve(FieldRefNode node) {
    ISourceRefType t = (ISourceRefType) node.getObject().resolveType();
    return t.findField(node.getId());
  }
  
  public IRegionBinding resolve(RegionNameNode node) {
    if (node.getParent() instanceof EffectSpecificationNode) {
      EffectSpecificationNode effect = (EffectSpecificationNode) node.getParent();
      IType t                        = effect.getContext().resolveType();
      return findRegionModel(t, node.getId());
    }
    
    // Need to handle from/to differently for region mappings
    if (node.getParent() instanceof RegionMappingNode) {
      RegionMappingNode mapping = (RegionMappingNode) node.getParent();
      if (node.equals(mapping.getFrom())) {
        IRNode vdecl = mapping.getPromisedFor();
        IRNode type  = VariableDeclarator.getType(vdecl);
        IJavaType jt = eb.getJavaType(type);
        return findRegionModel(jt, node.getId());
      }
    }
    final IRNode tdecl = findNearestType(node);
    return findRegionModel(tdecl, node.getId());
  }
  
  public IRegionBinding resolve(QualifiedRegionNameNode node) {
    final ISourceRefType type = node.getType().resolveType();
    return findRegionModel(type, node.getId());
  }

  private LockModel findLockModel(IRNode tdecl, String name) {
    LockModel o = eb.findClassBodyMembers(tdecl, new FindLockModelStrategy(eb, name), true);       
    return o;
  }
  
  public ILockBinding resolve(SimpleLockNameNode node) {
    final IRNode tdecl = findNearestType(node);
    return findLockModel(tdecl, node.getId());
  }
  
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
  public IRNode resolve(ColorImportNode node) {
 // TODO EDWIN LOOK HERE!
    // the ColorImportNode has an id String (accessible via getID()) that is either
    // the name of a Type or the name of a package.  When it's a type, we want to get
    // back the IRNode that is the foo_Decl of the type; for a package, we want the 
    // IRNode that is the package node of the package-info.java file for that package
    // (or, equivalently, the package node that represents the package-name.promises.xml
    // file for that package.
    final String importedName = node.getId();
    // try getting a package of that name
    IRNode res = tEnv.findPackage(importedName);
    if (res == null) {
      res = tEnv.findNamedType(importedName);
    }
   return res;
  }
  
  public IType resolveType(AASTNode node) {
    if (node instanceof IHasVariableBinding) {
      IHasVariableBinding ivb = (IHasVariableBinding) node;
      IVariableBinding vb     = ivb.resolveBinding();
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
  
  private ISourceRefType createISourceRefType(final IRNode type) {
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
      return new IType() {
        public IRNode getNode() {
          return eb.getBinding(type);
        }
        public IJavaType getJavaType() {
          return eb.getJavaType(type);
        }
      };
    }
    return null;
  }

  public IReferenceType resolveType(ReferenceTypeNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  public ISourceRefType resolveType(ClassTypeNode node) {
    if (node instanceof NamedTypeNode) {
      NamedTypeNode t = (NamedTypeNode) node;
      return createISourceRefType(resolveTypeName(t, t.getType()));
    }
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  public IPrimitiveType resolveType(PrimitiveTypeNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  public IVoidType resolveType(VoidTypeNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }

  public Iterable<ISourceRefType> resolveType(TypeDeclPatternNode node) {
    throw new UnsupportedOperationException("Auto-generated method stub: "+node.getClass().getName()); // TODO
  }
}
