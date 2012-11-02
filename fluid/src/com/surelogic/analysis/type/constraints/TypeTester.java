package com.surelogic.analysis.type.constraints;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

interface TypeTester {
  public boolean testArrayType(IJavaArrayType type);
  public ProofDrop testTypeDeclaration(IRNode type);
  public PromiseDrop<?> testFormalAgainstAnnotationBounds(IJavaTypeFormal formal);
}