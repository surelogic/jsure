/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/IBooleanPromiseDropStorage.java,v 1.3 2007/06/13 16:15:29 chance Exp $*/
package com.surelogic.promise;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.SlotInfo;

public interface IBooleanPromiseDropStorage<D extends PromiseDrop<?>> extends IPromiseDropStorage<D> {
  void init(SlotInfo<D> si);

  @Override
  SlotInfo<D> getSlotInfo();
}
