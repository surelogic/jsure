
package com.surelogic.jsure.tests;

import edu.cmu.cs.fluid.version.*;
import junit.framework.*;

public class TestPersistentLoad extends TestCase {
  public void testLoad() {
    int failures = TestPersistent.test("--load --zip --quiet --indirect index.arg "+
                        "--testdir ../build/plugins/fluid/src/edu/cmu/cs/fluid/version --test combined.tst");
    assertEquals(0, failures);
  }
}
