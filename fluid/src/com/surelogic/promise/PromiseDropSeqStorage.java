/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/PromiseDropSeqStorage.java,v 1.10 2007/08/30 14:19:11 chance Exp $*/
package com.surelogic.promise;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.util.*;

public final class PromiseDropSeqStorage<D extends PromiseDrop<?>> 
extends AbstractPromiseDropStorage<D>
implements IPromiseDropSeqStorage<D> {
  private SlotInfo<List<D>> si;
  
  protected PromiseDropSeqStorage(String name, Class<D> base) {
    super(name, base);
  }

  public static <P extends PromiseDrop<?>>
  PromiseDropSeqStorage<P> create(String name, Class<P> base) {
	  PromiseDropSeqStorage<P> s = new PromiseDropSeqStorage<P>(name, base);
	  PromiseDropStorage.register(s);
	  return s;
  }
  
  public StorageType type() {
    return StorageType.SEQ;
  }
  
  @Override
  public SlotInfo<List<D>> getSeqSlotInfo() {
    return si;
  }
  
  public void init(SlotInfo<List<D>> si) {
    checkSlotInfo(this.si, si);
    this.si = si;
  }
  
  public D add(IRNode n, D d) {
    checkArguments(n, d);
    List<D> l;
    if (!n.valueExists(si) || (l = n.getSlotValue(si)) == null) {
      l = new ArrayList<D>(); 
      n.setSlotValue(si, l);
    }
    /*
    // Clear out invalid drops
    Iterator<D> it = l.iterator();
    while (it.hasNext()) {
    	D old = it.next();
    	if (!old.isValid()) {
    		it.remove();
    	}
    }
    */
    if (l.contains(d)) {
      LOG.warning(d+" already associated with "+DebugUnparser.toString(n));
    } else {
      l.add(d);
    }
    return d;
  }
  
  public void remove(IRNode n, D d) {
    checkArguments(n, d);
    List<D> l = n.getSlotValue(si);
    if (!l.contains(d)) {
      throw new IllegalArgumentException("value not associated with node");
    }
    l.remove(d);
    if (l.isEmpty()) {
      n.setSlotValue(si, null);
    }
  }
  
  public boolean isDefined(IRNode n) {
    checkArgument(n);
    List<D> l = n.getSlotValue(si);    
    return l != null && !l.isEmpty();
  }
  
  public Iterable<D> getDrops(IRNode n) {
	  if (n == null) {
		  return new EmptyIterator<D>();
	  }
	  final List<D> l;
	  if (!n.valueExists(si) || (l = n.getSlotValue(si)) == null) {
		  return new EmptyIterator<D>();
	  }
	  return new ProcessIterator<D>(l.iterator()) {
		  @SuppressWarnings("unchecked")
		  @Override
		  protected Object select(Object o) { 
			  D d = (D) o;
			  if (d.isValid()) {
				  return d;
			  }
			  return notSelected; 
		  }
	  };
  }
}
