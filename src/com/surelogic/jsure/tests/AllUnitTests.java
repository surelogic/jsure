package com.surelogic.jsure.tests;

import junit.framework.TestSuite;
import edu.cmu.cs.fluid.analysis.structure.TestPackage;
import edu.cmu.cs.fluid.eclipse.bind.TestObjectBind;
import edu.cmu.cs.fluid.ir.TestIRPackage;
import edu.cmu.cs.fluid.java.analysis.TestJavaAnalysisPackage;
import edu.cmu.cs.fluid.sea.test.*;
import edu.cmu.cs.fluid.tree.TestTreePackage;
import edu.cmu.cs.fluid.util.TestMultiMap;
import edu.cmu.cs.fluid.util.TestUtilPackage;
import edu.cmu.cs.fluid.version.TestVersionPackage;

public class AllUnitTests extends TestSuite {

	public AllUnitTests() {
		addTest(new TestSuite(TestSea.class));
		addTest(new TestSuite(TestDrop.class));
		addTest(new TestSuite(TestPromiseDrop.class));

		addTest(new TestSuite(TestMultiMap.class));
		addTest(new TestSuite(TestUtilPackage.class));
		addTest(new TestSuite(TestIRPackage.class));
		addTest(new TestSuite(TestTreePackage.class));

		// Has some problems running with unversioned tests
		addTest(new TestSuite(TestVersionPackage.class));

		addTest(new TestSuite(TestObjectBind.class));

		addTest(new TestSuite(TestJavaAnalysisPackage.class));
		addTest(new TestSuite(TestPackage.class));
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
