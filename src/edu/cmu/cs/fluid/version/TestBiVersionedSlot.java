/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TestBiVersionedSlot.java,v 1.2 2007/01/23 20:33:08 chance Exp $*/
package edu.cmu.cs.fluid.version;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.Slot;


/**
 * Test bidirectional versioned slot.
 * @author boyland
 */
public class TestBiVersionedSlot {
  public void reportError(String msg) {
    System.out.println(msg);
  }
  public static void main(String[] args) {
    new TestBiVersionedSlot().test(args);
  }
  /**
   * @param args
   */
  void test(String[] args) {
    // ManyAssignedBiVersionedSlot.debugGetValue = true;
    Version v0 = Version.getVersion();
    Version.bumpVersion();
    Version v1 = Version.getVersion();
    Version.bumpVersion();
    Version v2 = Version.getVersion();
    Version.setVersion(v0);
    Version.bumpVersion();
    Version v3 = Version.getVersion();
    Version.bumpVersion();
    Version v4 = Version.getVersion();
    Version[] vs = new Version[]{v0,v1,v2,v3,v4};
    System.out.println("v0 = " + v0 + "; v1 = " + v1 + "; v2 = " + v2 + "; v3 = " + v3 + "; v4 = " + v4);
    
    Slot<Integer> s; 
    s = VersionedSlotFactory.bidirectional(v2).predefinedSlot(1);
    test("initially",s,vs,new int[] {1,1,1,1,1});
    Version.setVersion(v2);
    s = s.setValue(2);
    test("first assigned",s,vs,new int[] {2,2,2,2,2});
    s = s.setValue(3);
    test("re-assigned",s,vs,new int[] {3,3,3,3,3});
    Version.setVersion(v1);
    s = s.setValue(4);
    test("v1 assigned",s,vs,new int[] {4,4,3,4,4});
    s = s.setValue(5);
    test("v1 re-assigned",s,vs,new int[] {5,5,3,5,5});
    Version.setVersion(v0);
    s = s.setValue(6);
    test("v0 assigned",s,vs,new int[] {6,5,3,6,6});
    s = s.setValue(7);
    test("v0 re-assigned",s,vs,new int[] {7,5,3,7,7});
    Version.setVersion(v3);
    s = s.setValue(8);
    test("v3 assigned",s,vs,new int[] {7,5,3,8,8});
    s = s.setValue(9);
    test("v3 re-assigned", s,vs,new int[] {7,5,3,9,9});
    Version.setVersion(v4);
    s = s.setValue(0);  
    test("v4 assigned",s,vs,new int[] {7,5,3,9,0});
    
    // a new test
    s = VersionedSlotFactory.bidirectional(v2).predefinedSlot(1);
    Version.setVersion(v2);
    s = s.setValue(2);
    test("first assigned",s,vs,new int[] {2,2,2,2,2});
    s = s.setValue(3);
    test("re-assigned",s,vs,new int[] {3,3,3,3,3});
    Version.setVersion(v1);
    s = s.setValue(4);
    test("v1 assigned",s,vs,new int[] {4,4,3,4,4});
    s = s.setValue(5);
    test("v1 re-assigned",s,vs,new int[] {5,5,3,5,5});
    Version.setVersion(v0);
    s = s.setValue(6);
    test("v0 assigned",s,vs,new int[] {6,5,3,6,6});
    s = s.setValue(7);
    test("v0 re-assigned",s,vs,new int[] {7,5,3,7,7});
    Version.setVersion(v3);
    s = s.setValue(8);
    test("v3 assigned",s,vs,new int[] {7,5,3,8,8});
    s = s.setValue(9);
    test("v3 re-assigned", s,vs,new int[] {7,5,3,9,9});
    Version.setVersion(v4);
    s = s.setValue(0);  
    test("v4 assigned",s,vs,new int[] {7,5,3,9,0});

    s = VersionedSlotFactory.bidirectional(v2).predefinedSlot(1);
    Version.setVersion(v2);
    s = s.setValue(2);
    test("first assigned",s,vs,new int[] {2,2,2,2,2});
    Version.setVersion(v0);
    s = s.setValue(6);
    test("v0 assigned first",s,vs,new int[] {6,2,2,6,6});
    Version.setVersion(v4);
    s = s.setValue(0);  
    test("v4 assigned next",s,vs,new int[] {6,2,2,6,0});
    
    IRSequence<Integer> seq = VersionedSlotFactory.bidirectional(v0).newSequence(~0);
    int[] empty = new int[]{};
    int[] only3 = new int[]{3};
    int[] only4 = new int[]{4};
    int[] both = new int[]{3,4};
    
    test("empty",seq,vs,new int[][]{empty,empty,empty,empty,empty});
    Version.setVersion(v0);
    seq.appendElement(3);
    test("one added",seq,vs,new int[][]{only3,only3,only3,only3,only3});
    Version.setVersion(v1);
    seq.appendElement(4);
    test("second added",seq,vs,new int[][]{only3,both,both,only3,only3});
    Version.setVersion(v2);
    seq.removeElementAt(seq.firstLocation());
    test("first removed",seq,vs,new int[][]{only3,both,only4,only3,only3});
    
    seq = VersionedSlotFactory.bidirectional(v0).newSequence(~0);
    test("empty (again)",seq,vs,new int[][]{empty,empty,empty,empty,empty});
    Version.setVersion(v0);
    seq.appendElement(4);
    test("one appended",seq,vs,new int[][]{only4,only4,only4,only4,only4});
    Version.setVersion(v1);
    seq.insertElement(3);
    test("second inserted",seq,vs,new int[][]{only4,both,both,only4,only4});
    Version.setVersion(v2);
    seq.removeElementAt(seq.lastLocation());
    test("last removed",seq,vs,new int[][]{only4,both,only3,only4,only4});
    
    seq = VersionedSlotFactory.bidirectional(v2).newSequence(~0);
    test("empty (three)",seq,vs,new int[][]{empty,empty,empty,empty,empty});
    Version.setVersion(v1);
    seq.insertElement(3);
    test("one inserted",seq,vs,new int[][]{only3,only3,empty,only3,only3});
    Version.setVersion(v0);
    seq.appendElement(4);
    test("second appended",seq,vs,new int[][]{both,only3,empty,both,both});
    Version.setVersion(v4);
    seq.removeElementAt(seq.firstLocation());
    test("last removed (2)",seq,vs,new int[][]{both,only3,empty,both,only4});
    
    System.out.println("\n\nLast sequence tests.\n\n");
    seq = VersionedSlotFactory.bidirectional(v2).newSequence(~0);
    Version.setVersion(v2);
    seq.insertElement(3);
    test("initial [3]",seq,vs,new int[][]{only3,only3,only3,only3,only3});
    Version.setVersion(v1);
    seq.appendElement(4);
    test("4 appended",seq,vs,new int[][]{both,both,only3,both,both});
    Version.setVersion(v0);
    seq.removeElementAt(seq.firstLocation());
    test("3 removed",seq,vs,new int[][]{only4,both,only3,only4,only4});
    Version.setVersion(v4);
    seq.removeElementAt(seq.lastLocation());
    test("4 removed",seq,vs,new int[][]{only4,both,only3,only4,empty});
  }

  private void test(String test, Slot<Integer> s, Version[] vs, int[] vals) {
    for (int i=0; i < vs.length; ++i) {
      Version.saveVersion(vs[i]);
      int expected = vals[i];
      try {
        int value = s.getValue();
        if (expected != value) {
          reportError(test + ": At v" + i + " expected " + expected + " but got " + value);
          if (s instanceof ManyAssignedBiVersionedSlot) {
            reportError("  " + ((ManyAssignedBiVersionedSlot<Integer>)s).debugString());
          }
        }
      } catch (RuntimeException ex) {
        reportError("At v" + i + " expected " + expected + " but got exception " + ex);
        ex.printStackTrace();
      } finally {
        Version.restoreVersion();
      }
    }
  }
  
  private void test(String test, IRSequence<Integer> seq, Version[] vs, int[][] valss) {
    for (int i=0; i < vs.length; ++i) {
      Version.setVersion(vs[i]);
      int[] vals = valss[i];
      int j = 0;
      Iterator<Integer> it = seq.elements().iterator();
      while (j < vals.length && it.hasNext()) {
        int expected = vals[j];
        int actual = it.next();
        if (expected != actual) {
          reportError(test + ": At v" + i + " expected seq[" + j + "]=" + expected + " but got " + actual);
        }
        ++j;
      }
      while (it.hasNext()) {
        reportError(test + ": At v" + i + " got extra seq[" + j + "]=" + it.next());
        ++j;
      }
      while (j < vals.length) {
        reportError(test + ": At v" + i + " missed seq[" + j + "]=" + vals[j]);
        ++j;
      }
    }
  }
}
