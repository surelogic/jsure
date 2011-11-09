/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/BooleanPromiseDropStorage.java,v 1.7 2007/07/13 18:02:57 chance Exp $*/
package com.surelogic.promise;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;
import edu.cmu.cs.fluid.util.*;

public final class BooleanPromiseDropStorage<D extends BooleanPromiseDrop<?>> 
extends AbstractPromiseDropStorage<D>
implements IBooleanPromiseDropStorage<D> {
  private SlotInfo<D> si;
  
  protected BooleanPromiseDropStorage(String name, Class<D> base) {
    super(name, base);
  }
  
  public static <P extends BooleanPromiseDrop<?>>
  BooleanPromiseDropStorage<P> create(String name, Class<P> base) {
	  BooleanPromiseDropStorage<P> s = new BooleanPromiseDropStorage<P>(name, base);
	  PromiseDropStorage.register(s);
	  return s;
  }

  public StorageType type() {
    return StorageType.BOOLEAN;
  }
  
  @Override
  public SlotInfo<D> getSlotInfo() {
    return si;
  }
  
  public void init(SlotInfo<D> si) {
    checkSlotInfo(this.si, si);
    this.si = si;
  }

  public D add(IRNode n, D d) {
    checkArguments(n, d);
    D old = n.getSlotValue(si);
    if (old != null) {
      //System.out.println("Name: "+name()+" on "+n);
      throw new IllegalArgumentException("slot already defined");
    }
    n.setSlotValue(si, d);
    return d;
  }

  public void remove(IRNode n, D d) {
    checkArguments(n, d);
    D old = n.getSlotValue(si);
    if (old != d) {
      throw new IllegalArgumentException("value not associated with node");
    }
    n.setSlotValue(si, null);
  }

  public boolean isDefined(IRNode n) {
    checkArgument(n);
    D old = n.getSlotValue(si);
    return old != null;
  }
  
  public Iterable<D> getDrops(IRNode n) {
	  if (n == null) {
		  return EmptyIterator.prototype();
	  }
	  D d = n.getSlotValue(si);
	  if (!d.isValid()) {
		  return EmptyIterator.prototype();
	  }
	  return new SingletonIterator<D>(d);
  }
}
