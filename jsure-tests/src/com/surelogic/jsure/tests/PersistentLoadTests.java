/**
 * 
 */
package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

/**
 * @author Ethan.Urie
 *
 */
public class PersistentLoadTests extends TestSuite
{
	public PersistentLoadTests()
	{
		addTest(new TestSuite(TestPersistentLoad.class));
	}

	public static junit.framework.Test suite() {
		return new PersistentLoadTests();
	}
}
