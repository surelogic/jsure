package com.surelogic.analysis.concurrency.model;

public abstract class AbstractNeededLock implements NeededLock {
  protected final ModelLock<?, ?> modelLock;
  
  protected AbstractNeededLock(final ModelLock<?, ?> modelLock) {
    this.modelLock = modelLock;
  }
}
