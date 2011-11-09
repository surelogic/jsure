package com.surelogic.jsure.tests;

import junit.framework.TestCase;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.version.TestPersistent;

public class TestPersistentSave extends TestCase {
	@Override
	protected void setUp() {
		System.setProperty("fluid.ir.versioning", "Versioning.On");
		System.out.println("versioning = "+JJNode.versioningIsOn);
	}
	
	public void testSave() {
		int failures = TestPersistent
				.test("--store --quiet --zip --vic --trace "
						+ "--testdir junit-test-src/edu/cmu/cs/fluid/version --test all.tst");
		assertEquals(0, failures);
	}
}
