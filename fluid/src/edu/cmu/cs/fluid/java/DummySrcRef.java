package edu.cmu.cs.fluid.java;

public class DummySrcRef extends AbstractSrcRef {

  public static final ISrcRef undefined = new DummySrcRef();

  private DummySrcRef() {
    // no other instances
  }

  public String getCUName() {
    return null;
  }

  public Long getHash() {
    return null;
  }

  public String getPackage() {
    return null;
  }

  public String getProject() {
    return null;
  }
}
