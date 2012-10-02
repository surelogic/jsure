package com.surelogic.javac.adapter;

import edu.cmu.cs.fluid.java.ISrcRef;

public abstract class AbstractRef implements ISrcRef {

  protected final int f_line;

  AbstractRef(long l) {
    f_line = (int) l;
  }

  public int getLineNumber() {
    return f_line;
  }

  public String getJavaId() {
    return null;
  }

  public String getEnclosingJavaId() {
    return null;
  }
}
