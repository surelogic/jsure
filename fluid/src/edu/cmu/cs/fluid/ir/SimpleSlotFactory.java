/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SimpleSlotFactory.java,v 1.23 2008/06/26 20:20:45 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.fluid.util.EmptyIterator;

/** The family of simple (mutable and unversioned) slots. 
 * 
 * @lock ChangedLock is class protects stateChanged
 */
public class SimpleSlotFactory extends AbstractImplicitSlotFactory {
  protected SimpleSlotFactory() {}
  public static final SimpleSlotFactory prototype = new SimpleSlotFactory();
  static {
    IRPersistent.registerSlotFactory(prototype,'S');
  }

  @Override
  public <T> IRSequence<T> newSequence(int size) {
    if (size == 1) {
      return new Simple1IRStateArray<T>();
      //return new Simple1IRArray<T>();
    } else if (size > 1) {
      return new SimpleIRArray<T>(size);
    } else if (size < 0) {
      return new SimpleIRList<T>(~size);
    } else {
      return super.newSequence(size);
    }

  }
    
  public void noteChange(IRState state) {
    noteChanged(state);
  }
  
  private static Set<IRState> stateChanged = null;
  /**
   * Add this state to the simple state changed since the last request.
   * If no requests have been made yet, the information is tossed.
   * @param st simple state that has changed.
   */
  public static synchronized void noteChanged(IRState st) {
    if (stateChanged != null && !stateChanged.contains(st)) {
      stateChanged.add(st);
    }
  }
  /**
   * Return an iterator of everything changed since the last time we asked.
   * Note that this method clears the set (and also starts change recording if we haven't before).
   * @return
   */
  public static synchronized Iterator<IRState> getChanged() {
    Iterator<IRState> it;
    if (stateChanged == null) {
      it = new EmptyIterator<IRState>();
    } else {
      it = stateChanged.iterator();
    }
    stateChanged = new HashSet<IRState>();
    return it;
  }
}