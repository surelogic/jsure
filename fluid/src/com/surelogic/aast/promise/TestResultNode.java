/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/TestResultNode.java,v 1.2 2007/10/16 16:41:21 chance Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class TestResultNode {

  public static final AbstractAASTNodeFactory factory = 
    new AbstractAASTNodeFactory("TestResult") {
    @Override
    public AASTNode create(String _token, int _start, int _stop,
                           int _mods, String _id, int _dims, List<AASTNode> _kids) {
      throw new UnsupportedOperationException();  
    }
  };
}
