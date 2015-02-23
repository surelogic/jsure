/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/HistorySlot.java,v 1.4 2007/05/17 18:57:53 chance Exp $
 */
package edu.cmu.cs.fluid.ir;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A Slot that remember every time it is set.
 * @author boyland
 */
@SuppressWarnings("unchecked")
public class HistorySlot<T> extends AbstractSlot<T> {
  private List<T> history = new ArrayList();
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.Slot#getValue()
   */
  public T getValue() throws SlotUndefinedException {
    if (history.size() == 0) {
      throw new SlotUndefinedException("history slot not yet defined");
    }
    return history.get(history.size()-1);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.Slot#setValue(java.lang.Object)
   */
  public Slot setValue(T newValue) throws SlotImmutableException {
    history.add(newValue);
    return this;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.Slot#isValid()
   */
  public boolean isValid() {
    return history.size() > 0;
  }
  
  @Override
  public void describe(PrintStream out) {
    out.println("HistorySlot[" + history.size() + "]: ");
    for (Iterator<T> it = history.iterator(); it.hasNext();) {
      out.println("  " + it.next());
    }
  }
}
