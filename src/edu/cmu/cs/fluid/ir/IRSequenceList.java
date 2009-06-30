/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRSequenceList.java,v 1.5 2007/05/17 18:57:53 chance Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.LinkedList;
import java.util.List;
import java.util.AbstractSequentialList;
import java.util.ListIterator;
import java.util.NoSuchElementException;


/**
 * A wrapper that adapts an IRSequence as a {@link java.util.List}.
 * @author boyland
 */
public class IRSequenceList<T> extends AbstractSequentialList<T> implements List<T> {
  private final IRSequence<T> underlying;
  
  public IRSequenceList(IRSequence<T> seq) {
    underlying = seq;
  }

  private class Iterator implements ListIterator<T> {
    IRLocation loc;
    boolean forward = true;
    
    public Iterator(IRLocation l) {
      loc = l;
    }
    
    /* (non-Javadoc)
     * @see java.util.ListIterator#hasNext()
     */
    public boolean hasNext() {
      if (loc == null) return underlying.firstLocation() != null;
      return underlying.nextLocation(loc) != null;
    }
    
    /* (non-Javadoc)
     * @see java.util.ListIterator#next()
     */
    public T next() {
      forward = true;
      if (loc == null) loc = underlying.firstLocation();
      else loc = underlying.nextLocation(loc);
      if (loc == null) {
        throw new NoSuchElementException("next() at end of IRSequenceList");
      }
      return underlying.elementAt(loc);
    }
    
    /* (non-Javadoc)
     * @see java.util.ListIterator#hasPrevious()
     */
    public boolean hasPrevious() {
      return loc != null;
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#previous()
     */
    public T previous() {
      forward = false;
      if (loc == null) {
        throw new NoSuchElementException("previous() at start of IRSequenceList");
      }
      T result = underlying.elementAt(loc);
      loc = underlying.prevLocation(loc);
      return result;
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#nextIndex()
     */
    public int nextIndex() {
      if (loc == null) return 0;
      return underlying.locationIndex(loc)+1;
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#previousIndex()
     */
    public int previousIndex() {
      if (loc == null) return -1;
      return underlying.locationIndex(loc);
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#remove()
     */
    public void remove() {
      if (forward) {
        if (loc == null) throw new IllegalStateException("next() not called yet.");
        IRLocation prevLoc = underlying.prevLocation(loc);
        underlying.removeElementAt(loc);
        loc = prevLoc;
      } else {
        IRLocation nextLoc;
        if (loc == null) {
          nextLoc = underlying.firstLocation();
        } else {
          nextLoc = underlying.nextLocation(loc);
        }
        if (nextLoc == null) throw new IllegalStateException("already removed");
        underlying.removeElementAt(nextLoc);
      }
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#set(E)
     */
    public void set(T o) {
      if (forward) {
        if (loc == null) throw new IllegalStateException("next() not called yet.");
        underlying.setElementAt(o,loc);
      } else {
        IRLocation nextLoc;
        if (loc == null) {
          nextLoc = underlying.firstLocation();
        } else {
          nextLoc = underlying.nextLocation(loc);
        }
        if (nextLoc == null) throw new IllegalStateException("previous() not called yet.");
        underlying.setElementAt(o,nextLoc);
      }
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#add(E)
     */
    public void add(T o) {
      if (loc == null) loc = underlying.insertElement(o);
      else loc = underlying.insertElementAfter(o,loc);
    }
  }
  
  /* (non-Javadoc)
   * @see java.util.AbstractSequentialList#listIterator(int)
   */
  @Override
  public ListIterator<T> listIterator(int index) {
    if (index == 0) return new Iterator(null);
    else return new Iterator(underlying.location(index-1));
  }

  /* (non-Javadoc)
   * @see java.util.AbstractCollection#size()
   */
  @Override
  public int size() {
    return underlying.size();
  }
  
}

class TestIRSequenceList {
  public void reportError(String msg) {
    System.out.println("!!! "+msg);
  }
  
  public static void main(String args[]) {
    new TestIRSequenceList().test(args);
  }
  
  void test(String args[]) {
    IRSequence<String> s1 = SimpleSlotFactory.prototype.<String>newSequence(3);
    testSeq("fixed",s1, false);
    IRSequence<String> s2 = SimpleSlotFactory.prototype.<String>newSequence(~3);
    testSeq("variable",s2, false);
    IRSequence<String> s3 = SimpleSlotFactory.prototype.<String>newSequence(3);
    testSeq("fixed cast",s3, true);
    IRSequence<String> s4 = SimpleSlotFactory.prototype.<String>newSequence(~3);
    testSeq("variable cast",s4, true);
  }
  
  @SuppressWarnings("unchecked")
  private void testSeq(String kind, IRSequence<String> seq, boolean castToList) {
    for (IRLocation loc = seq.firstLocation(); loc != null; loc = seq.nextLocation(loc)) {
      seq.setElementAt(Integer.toString(seq.locationIndex(loc)),loc);
    }
    List<String> l1 = new LinkedList<String>(); 
    for (String s : seq.elements()) {
      l1.add(s);
    }
    List<String> l2 = castToList? (List<String>) seq : new IRSequenceList<String>(seq);
    
    testEq(kind + " initial",l1,l2);
    l1.set(0,"A"); l2.set(0,"A");
    testEq(kind + " change first",l1,l2);
    l1.set(l1.size()-1,"C"); l2.set(l2.size()-1,"C");
    testEq(kind + " change last",l1,l2);
    
    // test previous:
    ListIterator<String> lit1 = l1.listIterator(l1.size());
    ListIterator<String> lit2 = l2.listIterator(l2.size());
    while (lit1.hasPrevious() && lit2.hasPrevious()) {
      String p1 = lit1.previous();
      String p2 = lit2.previous();
      if (p1 != p2 && p1 == null || !p1.equals(p2)) {
        reportError("previous() returns different values");
      }
      lit1.set(p1+"-changed");
      lit2.set(p2+"-changed");
      testEq(kind + " changed in backward traversal",l1,l2);
    }
    if (lit1.hasPrevious() || lit2.hasPrevious()) {
      reportError("previous traversal yields different lengths");
    }
    
    if (kind.startsWith("fixed")) {
      System.out.println("Done with "+kind);
      return;    
    }
    
    l1.add(1,"new"); l2.add(1,"new");
    testEq(kind + " add new",l1,l2);
    l1.add("new end"); l2.add("new end");
    testEq(kind + " add new end",l1,l2);
    l1.remove(0); l2.remove(0);
    testEq(kind + " remove first",l1,l2);
    l1.remove(l1.size()-1); l2.remove(l2.size()-1);
    testEq(kind + " remove last",l1,l2);
    
    // test previous remove,add:
    lit1 = l1.listIterator(l1.size());
    lit2 = l2.listIterator(l2.size());
    boolean doRemove = true;
    while (lit1.hasPrevious() && lit2.hasPrevious()) {
      String p1 = lit1.previous();
      String p2 = lit2.previous();
      if (p1 != p2 && p1 == null || !p1.equals(p2)) {
        reportError("previous() returns different values");
      }
      if (doRemove) {
        lit1.remove();
        lit2.remove();
        testEq(kind + " remove in backward traversal",l1,l2);
      } else {
        lit1.add("added");
        lit2.add("added");
        testEq(kind + " add in backward traversal",l1,l2);
      }
      doRemove = !doRemove;
    }
    if (lit1.hasPrevious() || lit2.hasPrevious()) {
      reportError("previous traversal yields different lengths");
    }
    
    l1.add(0,"first"); l2.add(0,"first");
    testEq(kind + " insertion",l1,l2);
    l1.clear(); l2.clear();
    testEq(kind + " clear",l1,l2);
    
    System.out.println("Done with "+kind);
  }
  
  private void testEq(String test, List<String> s1, List<String> s2) {
    if (s1.equals(s2)) {
      if (s2.equals(s1)) {
        return;
      }
      reportError("Unsymmetric equality for " + s2 + " after test: " + test);
    } else if (s2.equals(s1)) {
      reportError("Unsymmetric equality for " + s1 + " after test: " + test);

      System.out.println("List iterator for "+s2);
      ListIterator<String> li = s2.listIterator();
      while (li.hasNext()) {
        System.out.println("\t"+li.next());
      }
    } else {
      reportError("Lost equality after test: " + test);
    }
  }
}
