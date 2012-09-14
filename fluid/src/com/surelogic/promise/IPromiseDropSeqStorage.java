/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/IPromiseDropSeqStorage.java,v 1.4 2007/06/18 18:22:21 chance Exp $*/
package com.surelogic.promise;

import java.util.List;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.sea.*;

public interface IPromiseDropSeqStorage<D extends PromiseDrop> 
extends IPromiseDropStorage<D> 
{
  void init(SlotInfo<List<D>> si);
  SlotInfo<List<D>> getSeqSlotInfo();
}
