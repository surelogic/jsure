package com.surelogic.analysis.type.constraints;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;

interface TypeTester {
  public boolean testArrayType(IJavaArrayType type);
  public ProofDrop testTypeDeclaration(IRNode type);
  public PromiseDrop<?> testFormalAgainstAnnotationBounds(IRNode formalDecl);
}