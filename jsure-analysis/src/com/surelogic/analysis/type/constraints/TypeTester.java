package com.surelogic.analysis.type.constraints;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;

import edu.cmu.cs.fluid.ir.IRNode;

interface TypeTester {
  public boolean testArrayType(IRNode arrayType);
  public ProofDrop testTypeDeclaration(IRNode type);
  public PromiseDrop<?> testFormalAgainstAnnotationBounds(IRNode formalDecl);
}