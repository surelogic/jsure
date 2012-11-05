package com.surelogic.jsure.tests;

import junit.framework.TestSuite;

import com.surelogic.common.TestFileUtility;
import com.surelogic.common.adhoc.model.TestColumnAnnotationParser;
import com.surelogic.common.ref.TestDecl;
import com.surelogic.common.ref.TestJavaRef;
import com.surelogic.common.xml.TestEntities;
import com.surelogic.dropsea.TestKeyValue;
import com.surelogic.dropsea.TestDrop;
import com.surelogic.dropsea.TestSea;

import edu.cmu.cs.fluid.ir.TestIRPackage;
import edu.cmu.cs.fluid.java.analysis.TestJavaAnalysisPackage;
import edu.cmu.cs.fluid.java.bind.TestJavaTypeCache2;
import edu.cmu.cs.fluid.tree.TestTreePackage;
import edu.cmu.cs.fluid.util.TestMultiMap;
import edu.cmu.cs.fluid.version.TestVersionPackage;

public class JUnitTestsRunByRegressionTest extends TestSuite {

  public JUnitTestsRunByRegressionTest() {
    // Keep in order with junit-test-src folder

    // com.surelogic.common
    addTest(new TestSuite(TestFileUtility.class));

    // com.surelogic.common.ref
    addTest(new TestSuite(TestJavaRef.class));
    addTest(new TestSuite(TestDecl.class));

    // com.surelogic.common.xml
    addTest(new TestSuite(TestEntities.class));

    // com.surelogic.dropsea
    addTest(new TestSuite(TestKeyValue.class));
    addTest(new TestSuite(TestSea.class));
    addTest(new TestSuite(TestDrop.class));

    // com.surelogic.common.adhoc.model
    addTest(new TestSuite(TestColumnAnnotationParser.class));

    // edu.cmu.cs.fluid.ir
    addTest(new TestSuite(TestIRPackage.class));

    // edu.cmu.cs.fluid.java.analysis
    addTest(new TestSuite(TestJavaAnalysisPackage.class));

    // edu.cmu.cs.fluid.java.bind
    addTest(new TestSuite(TestJavaTypeCache2.class));

    // edu.cmu.cs.fluid.tree
    addTest(new TestSuite(TestTreePackage.class));

    // edu.cmu.cd.fluid.util
    addTest(new TestSuite(TestMultiMap.class));

    // edu.cmu.cs.fluid.version
    addTest(new TestSuite(TestVersionPackage.class));
    // ^ Has some problems running with unversioned tests

    addTest(new TestSuite(TestTaskFramework.class));
    addTest(new TestSuite(TestSLAnnotationsParser.class));
    addTest(new TestSuite(SLParseTest.class));
    addTest(new TestSuite(TestScopedPromiseMatching.class));
    addTest(new TestSuite(TestAASTCloning.class));
  }

  public static junit.framework.Test suite() {
    return new JUnitTestsRunByRegressionTest();
  }
}