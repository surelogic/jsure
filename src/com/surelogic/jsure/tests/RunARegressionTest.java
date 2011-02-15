package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

public class RunARegressionTest extends TestSuite {

	public RunARegressionTest() {
		addTest(new TestSuite(RegressionTest.class));
	}

	public static junit.framework.Test suite() {
		return new RunARegressionTest();
	}
}
