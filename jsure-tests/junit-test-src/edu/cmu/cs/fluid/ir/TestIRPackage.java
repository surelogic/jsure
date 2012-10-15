package edu.cmu.cs.fluid.ir;

import com.surelogic.common.SLUtility;

import junit.framework.TestCase;

public class TestIRPackage extends TestCase {
  final String[] noArgs = SLUtility.EMPTY_STRING_ARRAY;
  
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
