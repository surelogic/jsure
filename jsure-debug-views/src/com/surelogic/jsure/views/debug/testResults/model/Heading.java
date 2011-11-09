package com.surelogic.jsure.views.debug.testResults.model;

import edu.cmu.cs.fluid.ir.IRNode;

public final class Heading {
  private final IRNode node;
  private boolean successful;
  
  public Heading(final IRNode n) {
    node = n;
    successful = true;
  }
  
  public IRNode getNode() {
    return node;
  }
  
  public void setFailed() {
    successful = false;
  }
  
  public boolean isSuccessful() {
    return successful;
  }
}
