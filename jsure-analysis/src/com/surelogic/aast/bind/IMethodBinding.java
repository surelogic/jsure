/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/bind/IVariableBinding.java,v 1.3 2007/06/29 18:07:38 chance Exp $*/
package com.surelogic.aast.bind;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IMethodBinding extends IBinding {
  IRNode getNode();
}
