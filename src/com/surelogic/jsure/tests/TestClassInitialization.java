package com.surelogic.jsure.tests;

import junit.framework.TestCase;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.test.*;
import com.surelogic.test.xml.*;

import edu.cmu.cs.fluid.eclipse.Eclipse;


/**
 * Check that classes are initialized in the right order
 * @author Edwin.Chan
 *
 */
public class TestClassInitialization extends TestCase {
  @Override
  protected void setUp() throws Exception {
    Eclipse.initialize(new Runnable() {
      public void run() {
        Eclipse.getDefault().addTestOutputFactory(JUnitXMLOutput.factory);
      }
    });

  }
  
  public void testLog() {
    System.err.println(">>>>>>>>>>>" + AnnotationRules.XML_LOG);
    System.err.println("Class: " + AnnotationRules.XML_LOG.getClass());
    assertTrue(AnnotationRules.XML_LOG instanceof MultiOutput);    
  }
}
