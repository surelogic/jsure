/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IAASTBinder.java,v 1.8 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.aast.*;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IAASTBinder {
  boolean isResolvable(AASTNode node);

  boolean isResolvable(ThisExpressionNode node);
  
  boolean isResolvable(QualifiedThisExpressionNode node);
  
  boolean isResolvable(VariableUseExpressionNode here);
  
  boolean isResolvable(FieldRefNode node);
  
  boolean isResolvable(RegionNameNode node);
  
  boolean isResolvable(QualifiedRegionNameNode node);
  
  boolean isResolvable(SimpleLockNameNode node);
  
  boolean isResolvable(QualifiedLockNameNode node);
  
  boolean isResolvableToType(AASTNode node);
  
  boolean isResolvable(ColorImportNode node);

  IBinding resolve(AASTNode node);

  IVariableBinding resolve(ThisExpressionNode node);
  
  IVariableBinding resolve(SuperExpressionNode node);
  
  IVariableBinding resolve(QualifiedThisExpressionNode node);

  IVariableBinding resolve(FieldRefNode node);

  IVariableBinding resolve(VariableUseExpressionNode node);

  IRegionBinding resolve(RegionSpecificationNode node);
  
  IRegionBinding resolve(RegionNameNode node);
  
  IRegionBinding resolve(QualifiedRegionNameNode node);

  ILockBinding resolve(SimpleLockNameNode node);
  
  ILockBinding resolve(QualifiedLockNameNode node);
  
  IRNode resolve(ColorImportNode node);

  IType resolveType(AASTNode node);

  ISourceRefType resolveType(ThisExpressionNode node);
  
  ISourceRefType resolveType(SuperExpressionNode node);

  IReferenceType resolveType(ReferenceTypeNode node);

  ISourceRefType resolveType(ClassTypeNode node);

  IPrimitiveType resolveType(PrimitiveTypeNode node);

  IVoidType resolveType(VoidTypeNode node);

  Iterable<ISourceRefType> resolveType(TypeDeclPatternNode node);
  
  
}
