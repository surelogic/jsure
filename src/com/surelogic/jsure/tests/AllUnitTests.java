package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

public class AllUnitTests extends TestSuite {

	public AllUnitTests() {
		//addTest(new TestSuite(TestSea.class));
		//addTest(new TestSuite(TestDrop.class));
		//addTest(new TestSuite(TestPromiseDrop.class));

		//addTest(new TestSuite(TestMultiMap.class));
		//addTest(new TestSuite(TestUtilPackage.class));
		//addTest(new TestSuite(TestIRPackage.class));
		//addTest(new TestSuite(TestTreePackage.class));

		// Has some problems running with unversioned tests
		//addTest(new TestSuite(TestVersionPackage.class));

		//addTest(new TestSuite(TestJavaAnalysisPackage.class));
		addTest(new TestSuite(TestTaskFramework.class));
        addTest(new TestSuite(TestSLAnnotationsParser.class));
		addTest(new TestSuite(SLParseTest.class));
		addTest(new TestSuite(TestScopedPromiseMatching.class));
		addTest(new TestSuite(TestAASTCloning.class));
	}

	public static junit.framework.Test suite() {
		return new AllUnitTests();
	}
}
