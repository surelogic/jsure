/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IR.java,v 1.3 2008/09/09 15:14:06 chance Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A class for global IR settings
 * 
 * @author Edwin.Chan
 */
public class IR {
  private static final List<IUndefinedSlotHandler> unHandlers = 
	new CopyOnWriteArrayList<IUndefinedSlotHandler>();
  
  public static void addUndefinedSlotHandler(IUndefinedSlotHandler h) {
    if (h == null || unHandlers.contains(h)) {
      return;
    }
    unHandlers.add(h);
  }
  
  @SuppressWarnings("unchecked")
  static boolean handleSlotUndefinedException(PersistentSlotInfo si, IRNode n) {
	  for(IUndefinedSlotHandler h : unHandlers) {
		  if (h.handleSlotUndefinedException(si, n)) {
			  return true;
		  }
	  }
	  return false;
  }
  
  public static void removeAllUndefinedSlotHandlers() {
	  unHandlers.clear();
  }
}
