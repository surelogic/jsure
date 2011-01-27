/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TestVersionPackage.java,v 1.3 2007/05/07 20:30:12 ethan Exp $*/
package edu.cmu.cs.fluid.version;

import junit.framework.TestCase;

public class TestVersionPackage extends TestCase {
  final String[] noArgs = new String[0];
  
  public void testBiVersionedSlot() {    
    TestBiVersionedSlot t = new TestBiVersionedSlot() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(noArgs);
  }
  
  public void testVersionedStructures() {    
    TestVersionedStructures t = new TestVersionedStructures() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(noArgs);
  }
  
  /*
  public void testVersionTest() {    
    VersionTest t = new VersionTest() {
      @Override public void reportError(String msg) {
        fail(msg);
      }
    };
    t.test(new String[] { "--verbose" });
  }
  */
}
