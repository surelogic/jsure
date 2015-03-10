/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IVariableBinding.java,v 1.3 2007/06/29 18:07:38 chance Exp $*/
package com.surelogic.aast.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;

public interface IVariableBinding extends IBinding {
  IRNode getNode();
  IJavaType getJavaType();
}
