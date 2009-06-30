/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRObservable.java,v 1.8 2008/09/05 19:56:21 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** A subclass of the standard observerable class where changes
 * are relative to an IRNode.  Normal observers are only notified
 * in the event that a value is changed.  But a special class of
 * observers are also notified when a value is defined that was
 * previously undefined.
 */
public class IRObservable extends Observable {
  private final CopyOnWriteArrayList<Observer> defineObservers = new CopyOnWriteArrayList<Observer>();
  private final AtomicBoolean changed = new AtomicBoolean(false);

  /** Add an observer that is also notified when a previously undefined
   * slot is given a value.
   */
  public void addDefineObserver(Observer o) {
    defineObservers.add(o);
  }

  /** Remove an observer from getting either normal or defining events.
   */
  public void deleteDefineObserver(Observer o) {
    defineObservers.remove(o);
  }

  /** Remove all defining and normal observers.
   */
  public void deleteDefineObservers() {
    defineObservers.clear();
  }
  
  public void notifyIRObservers(IRNode node) {
    setChanged();
    notifyObservers(node);
  }

  public void notifyDefineObservers(IRNode node) {
	  notifyDefineObservers_private(node);
  }
  
  private void notifyDefineObservers_private(Object arg) {
    if (defineObservers.size() == 0) {
      return;
    }
    for(Observer o : defineObservers) {
    	o.update(this, arg);    	
    }
  }
  
  @Override
  public void deleteObserver(Observer o) {
	  super.deleteObserver(o);
	  defineObservers.remove(o);
  }
  
  @Override
  public void deleteObservers() {
	  super.deleteObservers();
	  defineObservers.clear();
  }
  
  @Override
  public int countObservers() {
	  return super.countObservers() + defineObservers.size();
  }
  
  @Override
  public void notifyObservers(Object arg) {
	  boolean changed = hasChanged();
	  if (changed) {
		  if (super.countObservers() > 0) {
			  super.notifyObservers(arg);
		  }
	      notifyDefineObservers_private(arg);
	  }
  }
  
  @Override
  public void setChanged() {
	  changed.set(true);
  }
  
  @Override
  public void clearChanged() {
	  changed.set(false);
  }
  
  @Override
  public boolean hasChanged() {
	  return changed.get();  
  } 
}
