/*$Header: /cvs/fluid/fluid/src/com/surelogic/ast/fluid/FluidBinder.java,v 1.18 2007/06/04 15:30:46 chance Exp $*/
package com.surelogic.ast.fluid;

import java.util.*;

import com.surelogic.ast.*;
import com.surelogic.ast.IBinding;
import com.surelogic.ast.java.operator.*;
//import com.surelogic.ast.java.promise.*;
//import com.surelogic.proxy.java.operator.NodeFactories;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;

public class FluidBinder implements IJavaBinder {
  private static final Map<PrimitiveType,IPrimitiveType> prims = 
    new HashMap<PrimitiveType,IPrimitiveType>();
  static {
    prims.put(BooleanType.prototype, JavaTypeFactory.booleanType);
    prims.put(ByteType.prototype, JavaTypeFactory.byteType);
    prims.put(CharType.prototype, JavaTypeFactory.charType);
    prims.put(ShortType.prototype, JavaTypeFactory.shortType);
    prims.put(IntType.prototype, JavaTypeFactory.intType);
    prims.put(LongType.prototype, JavaTypeFactory.longType);
    prims.put(FloatType.prototype, JavaTypeFactory.floatType);
    prims.put(DoubleType.prototype, JavaTypeFactory.doubleType);
  }
  
  private final IBinder binder;
  public FluidBinder(IBinder b) {
    if (b == null) {
      throw new IllegalArgumentException("binder is null");
    }
    binder = b;
  }
  
  public List<IMethodDeclarationNode> getAllOverriddenMethods(
      IMethodDeclarationNode md) {
    return convertToList(binder.findOverriddenParentMethods((IRNode) md));
  }

  public IMethodDeclarationNode getOverriddenMethod(IMethodDeclarationNode md) {
    binder.findOverriddenParentMethods((IRNode) md);
    for (edu.cmu.cs.fluid.java.bind.IBinding pm : binder.findOverriddenParentMethods((IRNode) md)) {
      return (IMethodDeclarationNode) mapToBinding(pm.getNode());
    }
    // FIX Need to sort through the methods above
    return null;
  }

  public IConstructorDeclarationNode getSuperConstructor(IConstructorDeclarationNode cd) {
    throw new UnsupportedOperationException("There might not be just one, esp if it's compiled and the parent has multiple constructors");
  }

  public boolean isAssignmentCompatibleTo(ITypeDeclarationNode td, IType t) {
    if (isSubtypeOf(td, t)) {
      return true;
    }
    if (t instanceof ITypeFormal) {
      IRNode subT           = (IRNode) td;
      ITypeFormal tf = (ITypeFormal) t;      
      return binder.getTypeEnvironment().isSubType(JavaTypeFactory.getMyThisType(subT), 
                                                   JavaTypeFactory.getTypeFormal((IRNode) tf.getNode()));
    }
    if (t instanceof IWildcardType) {
      IWildcardType wb = (IWildcardType) t;          
      throw new UnsupportedOperationException("Used by declarations implementing Binding interface: "+wb);
    }
    return false;
  }

  public boolean isCastCompatibleTo(ITypeDeclarationNode td, IType t) {
    if (isSubtypeOf(td, t)) {
      return true;
    }
    throw new UnsupportedOperationException("Used by declarations implementing Binding interface");
  }

  public boolean isSubtypeOf(ITypeDeclarationNode td, IType t) {
    if (t instanceof IDeclaredType) {
      IRNode subT              = (IRNode) td;
      IDeclaredType sb = (IDeclaredType) t;
      IRNode supT              = (IRNode) sb.getNode();
      // FIX to check parameters
      return binder.getTypeEnvironment().isSubType(JavaTypeFactory.getMyThisType(subT), JavaTypeFactory.getMyThisType(supT));
    }
    return false;
  }

  public IFunctionBinding resolve(ISomeFunctionCallNode call) {
    IRNode decl = binder.getBinding((IRNode) call);
    return (IFunctionBinding) mapToBinding(decl);
  }

  public IConstructorBinding resolve(IConstructorCallNode call) {
    IRNode decl = binder.getBinding((IRNode) call);
    return (IConstructorBinding) mapToBinding(decl);
  }

  public IVariableBinding resolve(IFieldRefNode node) {
    IRNode decl = binder.getBinding((IRNode) node);
    return (IVariableBinding) mapToBinding(decl);
  }

  public IConstructorBinding resolve(IAllocationCallExpressionNode node) {
    IRNode decl = binder.getBinding((IRNode) node);
    return (IConstructorBinding) mapToBinding(decl);
  }

//  public ILockBinding resolve(ILockNameNode node) {
//    throw new UnsupportedOperationException("Can't resolve locks");
//  }

  public IAnnotationBinding resolve(IAnnotationNode node) {
    throw new UnsupportedOperationException("Can't resolve annotations");
  }

  public IVariableBinding resolve(IVariableUseExpressionNode node) {
    IRNode decl = binder.getBinding((IRNode) node);
    return (IVariableBinding) mapToBinding(decl);
  }

  public IFunctionBinding resolve(ICallNode node) {
    IRNode decl = binder.getBinding((IRNode) node);
    return (IFunctionBinding) mapToBinding(decl);
  }

  public IMethodBinding resolve(IMethodCallNode node) {
    IRNode decl = binder.getBinding((IRNode) node);
    return (IMethodBinding) mapToBinding(decl);
  }

  public IVariableBinding resolve(IReturnStatementNode node) {
    return null; // FIX
  }
  
//  public IRegionBinding resolve(IRegionNameNode node) {
//    throw new UnsupportedOperationException("Can't resolve regions");
//  }

  public ISourceRefType resolveType(ITypeRefNode node) {
    IRNode decl = binder.getBinding((IRNode) node);
    return (ISourceRefType) mapToBinding(decl);
  }

  public ISourceRefType resolveExtendsBound(ITypeFormalNode tf) {
    IRNode decl = binder.getBinding((IRNode) tf);
    return (ISourceRefType) mapToBinding(decl);
  }

  public IType resolveType(IReturnTypeNode t) {
    if (t instanceof IVoidTypeNode) {
      return IVoidType.VOID;
    }
    else if (t instanceof IPrimitiveTypeNode) {
      return resolveType((IPrimitiveTypeNode) t);
    }
    return resolveType((IReferenceTypeNode) t);
  }

  public INullType resolveType(INullLiteralNode e) {
    return INullType.NULL;
  }

  public IType resolveType(IEnumConstantDeclarationNode e) {
    // Need to return the enclosing enum decl
    IEnumDeclarationNode decl = (IEnumDeclarationNode) findEnclosingNode(e, BaseNodeType.ENUM_DECL);
    return decl;
  }
  
  public IType resolveType(IEnumConstantClassDeclarationNode e) {  
    return resolveType((IEnumConstantDeclarationNode) e);
  }
  
  public IType resolveType(IVariableDeclaratorNode e) {
    IVariableDeclListNode vdl = (IVariableDeclListNode) e.getParent(); 
    ITypeNode t = vdl.getType();
    IType tb = t.resolveType();
    if (tb == null) {
      System.out.println("tb == null");
    }
    return tb;
  }

  public IType resolveType(IParameterDeclarationNode e) {
    return e.getType().resolveType();
  }

  public IType resolveType(IExpressionNode e) {
    IJavaType jt = binder.getJavaType((IRNode) e);
    return convertToITypeBinding(jt);
  }

  public ISourceRefType resolveType(IClassTypeNode e) {
    IJavaType jt = binder.getJavaType((IRNode) e);
    return convertToISourceRefTypeBinding(jt);
  }

  public ISourceRefType resolveType(INamedTypeNode e) {
    IJavaType jt = binder.getJavaType((IRNode) e);
    return convertToISourceRefTypeBinding(jt);
  }
  
  public IReferenceType resolveType(IReferenceTypeNode t) {
    switch (t.getNodeType()) {
    case ARRAY_TYPE:
    case NAMED_TYPE:
    case PARAMETERIZED_TYPE:
    case TYPE_REF:
    case VAR_ARGS_TYPE:
    case WILDCARD_TYPE:
    case WILDCARD_EXTENDS_TYPE:
    case WILDCARD_SUPER_TYPE:
      IJavaType jt = binder.getJavaType((IRNode) t);
      return convertToReferenceTypeBinding(jt);
    case CLASS_TYPE: // Not expected to appear
    default:
    }
    throw new IllegalArgumentException("Unexpected ref type: "+t);
  }

  public IPrimitiveType resolveType(IPrimitiveTypeNode t) {
    switch (t.getNodeType()) {
    case BOOLEAN_TYPE:
      return JavaTypeFactory.booleanType;
    case FLOAT_TYPE:
      return JavaTypeFactory.floatType;
    case DOUBLE_TYPE:
      return JavaTypeFactory.doubleType;
    case BYTE_TYPE:
      return JavaTypeFactory.byteType;
    case CHAR_TYPE:
      return JavaTypeFactory.charType;
    case INT_TYPE:
      return JavaTypeFactory.intType;
    case LONG_TYPE:
      return JavaTypeFactory.longType;
    case SHORT_TYPE:
      return JavaTypeFactory.shortType;
    default:
    }
    throw new IllegalArgumentException("Unexpected prim type: "+t);
  }

  public IType resolveType(IVariableDeclarationNode n) {
    switch (n.getNodeType()) {
    case ENUM_CONSTANT_DECL:
      return resolveType((IEnumConstantDeclarationNode) n);
    case PARAMETER_DECL:
      return resolveType((IParameterDeclarationNode) n);
    case VARIABLE_DECLARATOR:
      return resolveType((IVariableDeclaratorNode) n);
    default:
    }
    throw new IllegalArgumentException("Unexpected node: "+n);
  }

  public IType resolveType(IMethodDeclarationNode n) {
    IRNode md = (IRNode) n;
    IJavaType jt = binder.getJavaType(MethodDeclaration.getReturnType(md));
    return convertToITypeBinding(jt);
  }

  public IVoidType resolveType(IVoidTypeNode e) {
    return IVoidType.VOID;
  }
  
  /*****************************************************************
   * Utility code to convert between ITypeBindings and IJavaType
   * 
   * Ideally, IJavaTypes would already be ITypeBindings
   *****************************************************************/
  
  private IType convertToITypeBinding(IJavaType jt) {
    if (jt instanceof IJavaVoidType) {
      return IVoidType.VOID; 
    }
    if (jt instanceof IJavaPrimitiveType) {
      PrimitiveType op = ((IJavaPrimitiveType) jt).getOp();
      return prims.get(op);
    }
    return handleReferenceType(jt);
  }

  private IReferenceType convertToReferenceTypeBinding(IJavaType jt) {
    if (jt instanceof IReferenceType) {
      return (IReferenceType) jt;
    }
    return handleReferenceType(jt);
  }
  
  private IReferenceType handleReferenceType(IJavaType jt) {
    IReferenceType rv = handleDeclaredType(jt);
    if (rv == null) {
      if (jt instanceof IJavaNullType) {
        return INullType.NULL;
      }
      else if (jt instanceof IJavaArrayType) {        
        return createArrayBinding((IJavaArrayType) jt);
      }
      else if (jt instanceof IJavaTypeFormal) {        
      }
      else if (jt instanceof IJavaWildcardType) {        
      }
      else if (jt instanceof IJavaCaptureType) {        
      }
    }
    return rv;
  }
  
  IReferenceType createArrayBinding(IJavaArrayType type) {
    return new ArrayTypeBinding(type);
  }
  
  private class ArrayTypeBinding implements IArrayType {
    private final IJavaArrayType at;
    private final IType base;
    
    private ArrayTypeBinding(IJavaArrayType type) {
      at   = type;
      base = convertToITypeBinding(type.getBaseType());
    }

    public int getDimensions() {
      return at.getDimensions();
    }
    
    public String getName() {
      return at.getName();
    }

    public IType getBaseType() {
      return base;
    }
    
    public IType getElementType() {
      IJavaType elt = at.getElementType();
      return convertToITypeBinding(elt);
    }
    
    private boolean couldBeCompatible(IType t) {
      if (t instanceof IArrayType) {
        IArrayType at2 = (IArrayType) t;
        return at.getDimensions() == at2.getDimensions() &&
         base.isSubtypeOf(at2.getBaseType());
      }
      else if (t instanceof IDeclaredType) {
        return getJavaLangObject().equals(t);
      }
      return false;
    }
    
    public boolean isAssignmentCompatibleTo(IType t) {
      return couldBeCompatible(t);
    }

    /*
    public boolean isCastCompatibleTo(IType t) {
      return couldBeCompatible(t);
    }
    */

    public boolean isSubtypeOf(IType t) {
      return couldBeCompatible(t);
    }

    public IDeclarationNode getNode() {
      return null;
    }
  }

  private IDeclaredType getJavaLangObject() {
    IRNode jlo              = binder.getTypeEnvironment().findNamedType("java.lang.Object");
    IDeclaredType dt = (IDeclaredType) mapToBinding(jlo);
    if (dt == null) {
      throw new Error("java.lang.Object == null");
    }
    return dt;
  }

  private ISourceRefType convertToISourceRefTypeBinding(IJavaType jt) {
    if (jt instanceof ISourceRefType) {
      return (ISourceRefType) jt;
    }
    return handleDeclaredType(jt);
  }
  
  private ISourceRefType handleDeclaredType(IJavaType jt) {
    if (jt instanceof IJavaDeclaredType) {
      IJavaDeclaredType dt = (IJavaDeclaredType) jt;
      return (ISourceRefType) mapToBinding(dt.getDeclaration());
    }
    return null;
  }
  
  private IJavaOperatorNode findEnclosingNode(IJavaOperatorNode n, INodeType toMatch) {
    IJavaOperatorNode here = n.getParent(); 
    while (here != null && here.getNodeType() != toMatch) {
      here = here.getParent();
    }
    return here;
  }
  
  @SuppressWarnings("unchecked")
  private <T> List<T> convertToList(Iterator<edu.cmu.cs.fluid.java.bind.IBinding> nodes) {
    if (nodes.hasNext()) {
      List<T> l = new ArrayList<T>();
      while (nodes.hasNext()) {
        l.add((T) nodes.next().getNode());
      }
    }
    return Collections.emptyList();
  }
  
  private IBinding mapToBinding(IRNode n) {
    return null;
    //return NodeFactories.getBinding(n);
  }
}