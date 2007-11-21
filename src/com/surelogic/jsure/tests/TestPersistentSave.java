package com.surelogic.jsure.tests;

import edu.cmu.cs.fluid.version.*;
import junit.framework.*;

public class TestPersistentSave extends TestCase {
  public void testSave() {
    int failures = TestPersistent.test("--store --quiet --zip --vic --trace "+
                        "--testdir ../build/plugins/fluid/src/edu/cmu/cs/fluid/version --test all.tst");
    assertEquals(0, failures);
  }
}
