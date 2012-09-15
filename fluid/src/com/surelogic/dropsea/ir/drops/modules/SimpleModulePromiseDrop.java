/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/modules/SimpleModulePromiseDrop.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.dropsea.ir.drops.modules;

import com.surelogic.aast.promise.ModuleChoiceNode;

public class SimpleModulePromiseDrop extends ModulePromiseDrop {
  SimpleModulePromiseDrop(ModuleChoiceNode mcn) {
    super(mcn, mcn.getModPromise().getModuleName());
  }
}
