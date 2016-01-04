package com.surelogic.analysis.effects;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Evidence that the effect comes from analyzing an object initialization 
 * control-flow on behalf of a particular constructor.
 */
public final class InitializationEffectEvidence implements EffectEvidence {
  private final IRNode constructorDecl;
  
  public InitializationEffectEvidence(final IRNode constructorDecl) {
    this.constructorDecl = constructorDecl;
  }

  public IRNode getConstructorDeclaration() {
    return constructorDecl;
  }

  
  
  @Override
  public void visit(final EffectEvidenceVisitor visitor) {
    visitor.visitInitializationEffectEvidence(this);
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * result + constructorDecl.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) { 
    if (other == this) {
      return true;
    } else if (other instanceof InitializationEffectEvidence) {
      final InitializationEffectEvidence o2 = (InitializationEffectEvidence) other;
      return this.constructorDecl.equals(o2.constructorDecl);
    }
    return false;
  }
}
