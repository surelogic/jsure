package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.MustInvokeOnOverrideNode;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.rules.StructureRules;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class MustInvokeOnOverridePromiseDrop extends BooleanPromiseDrop<MustInvokeOnOverrideNode> {

  public MustInvokeOnOverridePromiseDrop(MustInvokeOnOverrideNode a) {
    super(a);
    setCategory(Messages.DSC_LAYERS_ISSUES);
    setResultMessage(20, StructureRules.MUST_INVOKE_ON_OVERRIDE, JavaNames.getFullName(getNode()));
  }
}
