/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/AASTBinder.java,v 1.14 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.aast.*;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;

public abstract class AASTBinder implements IAASTBinder {
  private static IAASTBinder instance;

  public static IAASTBinder getInstance() {
    return instance;
  }
  
  public static void setInstance(IAASTBinder b ) {
    instance = b;
  }

  protected final IBinder binder;
  
  protected AASTBinder(IBinder b) {
    binder = b;
  }
  
  protected static IRNode findNearestType(IRNode here) {
    if (TypeDeclaration.prototype.includes(here)) {
      return here;
    }
    return VisitUtil.getEnclosingType(here);
  }
  
  protected static IRNode findNearestType(AASTNode here) {
    IRNode promisedFor    = here.getPromisedFor();
    //return VisitUtil.getClosestType(promisedFor);
    return findNearestType(promisedFor);
  }
  
  @Override
  public boolean isResolvable(RegionNameNode node) {
    return resolve(node) != null;
  }
  
  @Override
  public boolean isResolvable(QualifiedRegionNameNode node) {
    return resolve(node) != null;
  }
  
  @Override
  public boolean isResolvable(SimpleLockNameNode node) {
    return resolve(node) != null;
  }
  
  @Override
  public boolean isResolvable(QualifiedLockNameNode node) {
    return resolve(node) != null;
  }
  
  @Override
  public boolean isResolvable(ThreadRoleImportNode node) {
    return resolve(node) != null;
  }
  
  @Override
  public IRegionBinding resolve(RegionSpecificationNode node) {
    if (node instanceof RegionNameNode) {
      return resolve((RegionNameNode) node);
    } else {
      return resolve((QualifiedRegionNameNode) node);
    }
  }
  
  @Override
  public IVariableBinding resolve(VariableUseExpressionNode here) {
    IRNode promisedFor = here.getPromisedFor();
    final IRNode decl  = BindUtil.findLV(promisedFor, here.getId());
    if (decl == null) {
      return null;
    }
    return new IVariableBinding() {
      @Override
      public IRNode getNode() {
        return decl;
      }
      @Override
      public IJavaType getJavaType() {
        IRNode t = ParameterDeclaration.getType(decl);
        return binder.getJavaType(t);
      } 
    };
  }
  
  @Override
  public boolean isResolvable(VariableUseExpressionNode here) {
    return resolve(here) != null;
  }
   
  @Override
  public boolean isResolvable(ThisExpressionNode node) {
    return true;
  }
  
  @Override
  public boolean isResolvable(QualifiedThisExpressionNode node) {
	//return node.getType().typeExists(); 
	return resolve(node) != null;
  }
}
