/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/TestJavaTypeCache2.java,v 1.1 2007/04/13 18:20:04 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.MarkedIRNode;

public class TestJavaTypeCache2 extends TestCase {
    public void testPutGet() {
      JavaTypeCache2<IRNode,List<?>,JavaPrimitiveType> cache = new JavaTypeCache2<IRNode,List<?>,JavaPrimitiveType>();
      IRNode n = new MarkedIRNode("Test");
      cache.put(n, Collections.EMPTY_LIST, (JavaPrimitiveType) JavaTypeFactory.intType);

      JavaType result = cache.get(n, Collections.EMPTY_LIST);
      assertEquals(JavaTypeFactory.intType, result);
    }
  }
