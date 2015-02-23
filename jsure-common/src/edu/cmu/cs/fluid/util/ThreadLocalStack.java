/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/ThreadLocalStack.java,v 1.1 2007/11/05 15:03:10 chance Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.Stack;

/**
 * Designed to allow pushing pairs of different things
 * @author chance
 */
public class ThreadLocalStack<T> {
  static final Object noDefault = new Object();
  
  final ThreadLocal<Stack<T>> local = new ThreadLocal<Stack<T>>();  
  final T defaultValue;
  
  @SuppressWarnings("unchecked")
  public ThreadLocalStack() {
    defaultValue = (T) noDefault;
  }
  
  public ThreadLocalStack(T def) {
    defaultValue = def;
  }  
  
  public ThreadLocalStack<T> push(T e) { 
    push_internal(e); 
    return this;
  }
  
  Stack<T> push_internal(T e) {
    Stack<T> st = local.get();
    if (st == null) {
      st = new Stack<T>();
      local.set(st);
    }
    st.push(e);   
    return st;
  }
  
  public ThreadLocalStack<T> pushPair(T e1, T e2) {
    push_internal(e1).push(e2);
    return this;
  }
  
  T pop_internal(boolean popPair) {
    Stack<T> st = local.get();
    if (st == null || st.isEmpty()) {
      if (defaultValue != noDefault) {
        return defaultValue;
      }
      throw new UnsupportedOperationException();
    }
    if (popPair) {
      st.pop();
    }
    return st.pop();
  }
  
  public T pop() {
    return pop_internal(false);
  }
  
  public void popPair() {
    pop_internal(true);
  }
  
  public T peek() {
    return peek(0);
  }
  
  public T peek(int i) {
    Stack<T> st = local.get();
    if (st == null || st.isEmpty()) {
      if (defaultValue != noDefault) {
        return defaultValue;
      }
      throw new UnsupportedOperationException();
    }
    if (i == 0) {
      return st.peek();
    }
    else if (i < 0) {
      throw new IllegalArgumentException("index < 0: "+i);
    }
    return st.get(st.size() - i - 1);
  }
}
