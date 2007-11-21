/**
 * 
 */
package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

/**
 * @author Ethan.Urie
 *
 */
public class AllRegressionTests extends TestSuite
{
	public AllRegressionTests()
	{
		addTest(new TestSuite(RegressionTest.class));
	}

	public static junit.framework.Test suite() {
		return new AllRegressionTests();
	}
}
