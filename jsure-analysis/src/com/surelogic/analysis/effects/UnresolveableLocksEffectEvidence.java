package com.surelogic.analysis.effects;

import java.util.Set;

import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

/**
 * Evidence that the effect comes from a method call, and that call has
 * lock preconditions that cannot be resolved in the calling context.  This
 * occurs when the lock precondition names a lock on an outer object.  This can
 * be resolved within the inner class that declares the method, but not when
 * the method is called from other contexts.
 */
public final class UnresolveableLocksEffectEvidence implements EffectEvidence {
  private final RequiresLockPromiseDrop requiresLock;
  private final Set<LockSpecificationNode> unresolveableSpecs;
  
  public UnresolveableLocksEffectEvidence(
      final RequiresLockPromiseDrop requiresLock,
      final Set<LockSpecificationNode> unresolveableSpecs) {
    this.requiresLock = requiresLock;
    this.unresolveableSpecs = unresolveableSpecs;
  }

  public RequiresLockPromiseDrop getRequiresLock() {
    return requiresLock;
  }
  
  public Set<LockSpecificationNode> getUnresolveableSpecs() {
    return unresolveableSpecs;
  }

  
  
  @Override
  public void visit(final EffectEvidenceVisitor visitor) {
    visitor.visitUnresolveableLocksEffectEvidence(this);
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * result + requiresLock.hashCode();
    result += 31 * result + unresolveableSpecs.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) { 
    if (other == this) {
      return true;
    } else if (other instanceof UnresolveableLocksEffectEvidence) {
      final UnresolveableLocksEffectEvidence o2 = (UnresolveableLocksEffectEvidence) other;
      return this.requiresLock.equals(o2.requiresLock) &&
          this.unresolveableSpecs.equals(o2.unresolveableSpecs);
    }
    return false;
  }
}
