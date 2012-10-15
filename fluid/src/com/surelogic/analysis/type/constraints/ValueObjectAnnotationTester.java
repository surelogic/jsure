package com.surelogic.analysis.type.constraints;

import java.util.Map;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.EqualityRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ValueObjectPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ValueObjectAnnotationTester extends TypeDeclAnnotationTester {
  public ValueObjectAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, 
      final Map<IJavaType, ResultFolderDrop> folders, final boolean ex) {
    super(binder, fe, folders, ex);
  }
  
  @Override
  protected ValueObjectPromiseDrop testTypeDeclaration(final IRNode type) {
    return EqualityRules.getValueObjectDrop(type);
  }           
  
  @Override
  protected Set<PromiseDrop<? extends IAASTRootNode>> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isValueObject(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
