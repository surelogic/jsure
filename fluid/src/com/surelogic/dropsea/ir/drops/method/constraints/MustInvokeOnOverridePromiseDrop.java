package com.surelogic.dropsea.ir.drops.method.constraints;

import com.surelogic.aast.promise.MustInvokeOnOverrideNode;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.rules.StructureRules;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaNames;

public final class MustInvokeOnOverridePromiseDrop extends BooleanPromiseDrop<MustInvokeOnOverrideNode> {

  public MustInvokeOnOverridePromiseDrop(MustInvokeOnOverrideNode a) {
    super(a);
    setCategorizingString(Messages.DSC_LAYERS_ISSUES);
    setMessage(20, StructureRules.MUST_INVOKE_ON_OVERRIDE, JavaNames.getFullName(getNode()));
  }
}
