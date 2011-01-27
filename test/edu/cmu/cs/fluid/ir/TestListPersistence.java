/*
 * CompSci 552
 * Homework #3
 * Solution
 * John Boyland
 * Fall 2005
 */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;


/**
 * Test case for persistence that illustrates the bug reported as
 * <a href="http://www.fluid.cs.cmu.edu/bugzilla/show_bug.cgi?id=320">Bug #320</a>.
 * @author boyland
 */
public class TestListPersistence {
  public void reportError(String msg) {
    System.err.println("!! " + msg);
  }
  public static void main(String[] args) {
    new TestListPersistence().test(args);
  }
  void test(String[] args) {
    System.out.println("TestListPersistence");
    IRSequence<Integer> l1 = SimpleExplicitSlotFactory.prototype.newSequence(-1);
    l1.appendElement(10);
    l1.appendElement(20);
    l1.appendElement(30);
    IRSequence<Integer> l2 = SimpleExplicitSlotFactory.prototype.newSequence(-1);
    l2.insertElement(3);
    l2.insertElement(2);
    l2.insertElement(1);
    IRSequenceType<Integer> st = new IRSequenceType<Integer>(IRIntegerType.prototype);
    passThru("append",l1,st);
    passThru("insert",l2,st);
    l2.appendElement(4);
    passThru("mixed",l2,st);
    System.out.println("Test Done.");
  }
  
  private void passThru(String test,IRSequence<Integer> s,IRSequenceType<Integer> st) {
    IRPipe pipe = new IRPipe();
    IRSequence<Integer> copy;
    try {
      st.writeValue(s,pipe);
      copy = st.readValue(pipe);
    } catch (IOException e) {
      assert false;
      return;
    } catch (RuntimeException e) {
      reportError(test + ": Exception on pipe I/O: " + e);
      e.printStackTrace();
      return;
    }
    if (copy.size() != s.size()) {
      reportError(test + ": Copied has a new size: "
          + copy.size() + " != " + s.size());
    }
    for (int i=0; i < s.size(); ++i) {
      Object o1 = s.elementAt(i);
      Object o2 = copy.elementAt(i);
      if (o1 == null) {
        if (o2 != null) reportError(test + ": Expected null as element[" + i + "], but got " + o2);
      } else {
        if (!o1.equals(o2)) {
          reportError(test + ": Expected " + o1 + " as element[" + i + "], but got " + o2);
        }
      }
    }
  }
}
