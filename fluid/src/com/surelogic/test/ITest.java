/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/ITest.java,v 1.3 2007/08/30 18:41:45 chance Exp $*/
package com.surelogic.test;

import edu.cmu.cs.fluid.ir.IRNode;

public interface ITest {
  /**
   * @return The context being tested
   */
  IRNode getNode();
  
  /**
   * @return The class being tested
   */
  String getClassName();
  
  String identity();
}
