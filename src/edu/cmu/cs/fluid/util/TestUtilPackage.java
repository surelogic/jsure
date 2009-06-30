/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/TestUtilPackage.java,v 1.1 2007/01/23 19:47:56 chance Exp $*/
package edu.cmu.cs.fluid.util;

import java.io.IOException;

import junit.framework.TestCase;

public class TestUtilPackage extends TestCase {
  final String[] noArgs = new String[0];

  public void testHashtable2() {    
    TestHashtable2 t = new TestHashtable2(0) {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(noArgs);
  }
  
  public void testBase64() {    
    TestBase64 t = new TestBase64() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    try {
      t.test(noArgs);
    } catch (IOException e) {
      fail(e.getMessage());
      e.printStackTrace();
    }
  }

  public void testSet() {
    TestSet t = new TestSet(0) {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(new String[] { "1", "Set"});
    t.test(new String[] { "1", "CachedSet"});
    t.test(new String[] { "1", "UnionLattice"});
    t.test(new String[] { "1", "IntersectionLattice"});
  }
  
  public void testIntegerTable() {    
    TestIntegerTable t = new TestIntegerTable() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(new String[] { "5", "7", "100", "2", "11" });
  }
  
  public void testImmutableList() {    
    TestImmutableList t = new TestImmutableList() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(noArgs);
  }
}

