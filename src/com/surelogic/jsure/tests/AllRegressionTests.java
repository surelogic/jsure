package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

public class AllRegressionTests extends TestSuite {
	public AllRegressionTests() {
		System.out.println("CONSTRUCTING ALLREGRESSIONTESTS");
		
		addTest(new TestSuite(RegressionTest.class));
	}

	public static junit.framework.Test suite() {
		return new AllRegressionTests();
	}
}
