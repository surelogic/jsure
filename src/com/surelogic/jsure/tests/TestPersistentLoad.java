package com.surelogic.jsure.tests;

import junit.framework.TestCase;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.version.TestPersistent;

public class TestPersistentLoad extends TestCase {
	@Override
	protected void setUp() {
		System.setProperty("fluid.ir.versioning", "Versioning.On");
		System.out.println("versioning = "+JJNode.versioningIsOn);
	}
	
	public void testLoad() {
		int failures = TestPersistent
				.test("--load --zip --quiet --indirect index.arg "
						+ "--testdir junit-test-src/edu/cmu/cs/fluid/version --test combined.tst");
		assertEquals(0, failures);
	}
}
