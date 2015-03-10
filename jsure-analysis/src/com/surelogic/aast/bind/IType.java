/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IType.java,v 1.4 2007/06/28 16:49:04 chance Exp $*/
package com.surelogic.aast.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;

public interface IType {
  IRNode getNode();
  IJavaType getJavaType();
}
