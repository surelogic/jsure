package edu.cmu.cs.fluid.ir;

import junit.framework.TestCase;

public class TestIRPackage extends TestCase {
  final String[] noArgs = new String[0];
  
  public void testIRSequenceList() {    
    TestIRSequenceList t = new TestIRSequenceList() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(noArgs);
  }
  
  public void testListPersistence() {    
    TestListPersistence t = new TestListPersistence() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(noArgs);
  }
}
