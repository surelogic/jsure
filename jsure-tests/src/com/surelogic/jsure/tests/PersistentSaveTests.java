/**
 * 
 */
package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

/**
 * @author Ethan.Urie
 *
 */
public class PersistentSaveTests extends TestSuite
{
	public PersistentSaveTests()
	{
		addTest(new TestSuite(TestPersistentSave.class));
	}

	public static junit.framework.Test suite() {
		return new PersistentSaveTests();
	}
}
