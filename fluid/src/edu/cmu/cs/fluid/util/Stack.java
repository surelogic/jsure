/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Stack.java,v 1.5 2008/10/27 19:49:01 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;

public class Stack<T> implements Iterable<T> {
  Vector<T> contents = new Vector<T>();
  public Stack() {
	  // Nothing to do
  }

  public boolean isEmpty() {
    return contents.isEmpty();
  }

  public int size() {
    return contents.size();
  }

  public void push(T element) {
    contents.addElement(element);
  }

  public T pop() throws NoSuchElementException {
    T element = contents.lastElement();
    contents.removeElementAt(contents.size()-1);
    return element;
  }
  
  public Collection<T> removeAll() {
	  Vector<T> temp = contents; 
	  contents = new Vector<T>();
	  return temp;
  }

  public Iterator<T> iterator() {
	return contents.iterator();
  }
}

