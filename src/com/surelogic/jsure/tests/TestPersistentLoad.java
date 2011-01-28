package com.surelogic.jsure.tests;

import junit.framework.TestCase;
import edu.cmu.cs.fluid.version.TestPersistent;

public class TestPersistentLoad extends TestCase {
	public void testLoad() {
		int failures = TestPersistent
				.test("--load --zip --quiet --indirect index.arg "
						+ "--testdir ../build/plugins/fluid/src/edu/cmu/cs/fluid/version --test combined.tst");
		assertEquals(0, failures);
	}
}
