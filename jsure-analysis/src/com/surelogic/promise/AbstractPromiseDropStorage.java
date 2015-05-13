/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/AbstractPromiseDropStorage.java,v 1.3 2007/07/13 18:02:57 chance Exp $*/
package com.surelogic.promise;

import java.util.List;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

public abstract class AbstractPromiseDropStorage<D extends PromiseDrop<?>> implements IPromiseDropStorage<D> {
  protected Logger LOG = SLLogger.getLogger("fluid");

  private final String name;
  private final Class<D> base;

  protected AbstractPromiseDropStorage(String name, Class<D> base) {
    this.name = name;
    this.base = base;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final Class<D> baseDropType() {
    return base;
  }

  @Override
  public SlotInfo<List<D>> getSeqSlotInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SlotInfo<D> getSlotInfo() {
    throw new UnsupportedOperationException();
  }

  protected final void checkSlotInfo(SlotInfo<?> oldSI, SlotInfo<?> si) {
    if (si == null) {
      throw new IllegalArgumentException("null SlotInfo");
    }
    if (oldSI != null) {
      throw new IllegalArgumentException("SlotInfo already defined");
    }
  }

  protected void checkArguments(IRNode n, D d) {
    if (n == null || d == null) {
      throw new IllegalArgumentException();
    }
  }

  protected void checkArgument(IRNode n) {
    if (n == null) {
      throw new IllegalArgumentException();
    }
  }
}
