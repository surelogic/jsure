/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/SinglePromiseDropStorage.java,v 1.6 2007/07/13 18:02:57 chance Exp $*/
package com.surelogic.promise;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

public final class SinglePromiseDropStorage<D extends PromiseDrop<?>> extends AbstractPromiseDropStorage<D> implements
    ISinglePromiseDropStorage<D> {
  private SlotInfo<D> si;

  protected SinglePromiseDropStorage(String name, Class<D> base) {
    super(name, base);
  }

  public static <P extends PromiseDrop<?>> SinglePromiseDropStorage<P> create(String name, Class<P> base) {
    SinglePromiseDropStorage<P> s = new SinglePromiseDropStorage<P>(name, base);
    PromiseDropStorage.register(s);
    return s;
  }

  @Override
  public StorageType type() {
    return StorageType.NODE;
  }

  @Override
  public SlotInfo<D> getSlotInfo() {
    return si;
  }

  @Override
  public void init(SlotInfo<D> si) {
    checkSlotInfo(this.si, si);
    this.si = si;
  }

  @Override
  public D add(IRNode n, D d) {
    checkArguments(n, d);
    if (n.valueExists(si)) {
      D d2 = n.getSlotValue(si);
      if (d2 != null && !d.equals(d2)) {
        throw new IllegalArgumentException("slot already defined");
      }
    }
    n.setSlotValue(si, d);
    return d;
  }

  @Override
  public void remove(IRNode n, D d) {
    checkArguments(n, d);
    D old = n.getSlotValue(si);
    if (old != d) {
      throw new IllegalArgumentException("value not associated with node");
    }
    n.setSlotValue(si, null);
  }

  @Override
  public boolean isDefined(IRNode n) {
    checkArgument(n);
    D old = n.getSlotValue(si);
    return old != null;
  }

  @Override
  public Iterable<D> getDrops(IRNode n) {
    if (n == null) {
      return new EmptyIterator<D>();
    }
    D d = n.getSlotValue(si);
    if (d == null || !d.isValid()) {
      return new EmptyIterator<D>();
    }
    return new SingletonIterator<D>(d);
  }
}
