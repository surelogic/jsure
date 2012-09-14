/*$Header: /cvs/fluid/com.surelogic.jsure.tests/src/com/surelogic/test/xml/TestJUnitXMLFormatter.java,v 1.2 2007/08/02 20:33:44 chance Exp $*/
package com.surelogic.test.xml;

import junit.framework.*;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import com.surelogic.ant.junit.*;

public class TestJUnitXMLFormatter {
  public static void main(String[] args) {
    XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
    JUnitTest test = new JUnitTest("TestJUnitXMLFormatter");
    test.setProperties(System.getProperties());     
    // Get Eclipse properties?
    
    formatter.setOutput(System.out);
    formatter.startTestSuite(test);

    long start = System.currentTimeMillis();
    testOne(formatter);
    testOne(formatter);
    test.setRunTime(System.currentTimeMillis() - start);
    test.setCounts(2, 0, 2);    
    formatter.endTestSuite(test);    
    System.out.println(test.getRunTime());
  }

  private static void testOne(XMLJUnitResultFormatter formatter) {
    //the trick to integrating test output to the formatter, is to
    //create a special test class that asserts a timout occurred,
    //and tell the formatter that it raised.  
    Test t = new Test() {
     	    @Override
            public int countTestCases() { return 1; }
    	    @Override
            public void run(TestResult r) {
                throw new AssertionFailedError("Timeout occurred");
            }
        };
    formatter.startTest(t);
    formatter.addError(t, new AssertionFailedError("Timeout occurred"));
  }
}
