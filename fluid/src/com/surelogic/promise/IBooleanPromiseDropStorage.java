/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/IBooleanPromiseDropStorage.java,v 1.3 2007/06/13 16:15:29 chance Exp $*/
package com.surelogic.promise;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.sea.*;

public interface IBooleanPromiseDropStorage<D extends PromiseDrop> 
extends IPromiseDropStorage<D> 
{
  void init(SlotInfo<D> si);
  SlotInfo<D> getSlotInfo();
}
