/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/modules/ModuleWrapperPromiseDrop.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.dropsea.ir.drops.promises.modules;

import com.surelogic.aast.promise.ModuleChoiceNode;

public class ModuleWrapperPromiseDrop extends ModulePromiseDrop {
  ModuleWrapperPromiseDrop(ModuleChoiceNode mcn) {
    super(mcn, mcn.getModWrapper().getModuleName());
  }

}
