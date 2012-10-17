package com.surelogic.analysis.type.constraints;

import java.util.Map;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.EqualityRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;

public final class ReferenceObjectAnnotationTester extends TypeDeclAnnotationTester {
  private static final int ENUM_IMPLICITLY_REF_OBJECT = 764;
  
  private static final String JAVA_LANG_ENUM = "java.lang.Enum";
  
  
  
  public ReferenceObjectAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, 
      final Map<IJavaType, ResultFolderDrop> folders, final boolean ex) {
    super(binder, fe, folders, ex);
  }
  
  @Override
  protected ProofDrop testTypeDeclaration(final IRNode type) {
    if (EnumDeclaration.prototype.includes(type) || JavaNames.getFullTypeName(type).equals(JAVA_LANG_ENUM)) {
      final ResultDrop result = new ResultDrop(type);
      result.setConsistent();
      result.setMessage(ENUM_IMPLICITLY_REF_OBJECT, JavaNames.getRelativeTypeNameDotSep(type));
      return result;
    } else {
      return EqualityRules.getRefObjectDrop(type);
    }
  }           
  
  @Override
  protected Set<PromiseDrop<? extends IAASTRootNode>> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isReferenceObject(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
