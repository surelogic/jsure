package com.surelogic.analysis.concurrency.model;

public final class NeededStaticLock extends AbstractNeededLock {
  public NeededStaticLock(final ModelLock<?, ?> modelLock) {
    super(modelLock);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * modelLock.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof NeededStaticLock) {
      final NeededStaticLock o = (NeededStaticLock) other;
      return this.modelLock.equals(o.modelLock);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return modelLock.getImplementation().getClassName() + 
        modelLock.getImplementation().getPostfixId();
  }
}
