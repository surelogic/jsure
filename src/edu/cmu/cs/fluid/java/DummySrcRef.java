/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/DummySrcRef.java,v 1.5 2008/06/30 15:23:12 chance Exp $*/
package edu.cmu.cs.fluid.java;

public class DummySrcRef extends AbstractSrcRef {
  public static final ISrcRef prototype = new DummySrcRef();
  public static final ISrcRef undefined = new DummySrcRef();
  
  private DummySrcRef() {
	  // Nothing to do
  }

  public String getCUName() {
	  // TODO Auto-generated method stub
	  return null;
  }

  public Long getHash() {
	  // TODO Auto-generated method stub
	  return null;
  }

  public String getPackage() {
	  // TODO Auto-generated method stub
	  return null;
  }

  public String getProject() {
	  // TODO Auto-generated method stub
	  return null;
  }
}
