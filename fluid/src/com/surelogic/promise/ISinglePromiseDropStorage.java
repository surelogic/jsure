/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/ISinglePromiseDropStorage.java,v 1.4 2007/06/18 18:22:21 chance Exp $*/
package com.surelogic.promise;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.SlotInfo;

public interface ISinglePromiseDropStorage<D extends PromiseDrop> 
extends IPromiseDropStorage<D> 
{
  void init(SlotInfo<D> si);
  SlotInfo<D> getSlotInfo();
}
