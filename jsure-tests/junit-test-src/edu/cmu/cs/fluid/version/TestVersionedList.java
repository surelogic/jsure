/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TestVersionedList.java,v 1.7 2007/05/25 02:12:41 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.*;

import edu.cmu.cs.fluid.ir.*;

/**
 * TODO Fill in purpose.
 * @author boyland
 */
public class TestVersionedList {
  static IRSequence<Object> seq;
  
  private static void testPersistentError() {
    // this error was noticed in TestPersistent:
    seq = VersionedSlotFactory.prototype.newSequence(~2);
    seq.setElementAt(new Integer(4), 0);
    seq.setElementAt(new Integer(10), 1);
    Version v0 = Version.getVersion();
    seq.appendElement(new Integer(7));
    try {
      System.out.println("Passed test: " + seq.elementAt(2));
    } catch (SlotUndefinedException x) {
      System.out.println("!!! Didn't get second element in new version.");
      printSeq("after append: ");
    } catch (IndexOutOfBoundsException e) {
      System.out.println("!! Couldn't find last element in new version.");
      printSeq("after append: ");
    }
    Version.setVersion(v0);
    try {
      System.out.println("!!! second element is " + seq.elementAt(2));
      printSeq("undoing append");
    } catch (SlotUndefinedException x) {
      System.out.println("!!! Wrong exception");
      printSeq("undoing append");
    } catch (IndexOutOfBoundsException e) {
      System.out.println("Passed test.");
    }
  }
  
  private static void printSeq(PrintStream pw) {
    pw.print(seq.size() + "[");
    boolean started = false;
    for (IRLocation loc = seq.firstLocation(); loc != null; loc = seq.nextLocation(loc)) {
      if (started) {
        pw.print(",");
      } else {
        started = true;
      }
      if (seq.validAt(loc)) {
        pw.print(seq.elementAt(loc));
      } else {
        pw.print("?");
      }
    }
    pw.print("]");
  }
  /*private static void printSeq(Version v, PrintStream pw) {
    Version.saveVersion(v);
    try {
      printSeq(pw);
    } finally {
      Version.restoreVersion();
    }
  }*/
  private static void printSeq(String prefix) {
    System.out.print(prefix + ": ");
    printSeq(System.out);
    System.out.println();
    seq.describe(System.out);
  }
  
  public static void main(String[] args) {
    testPersistentError();
  }
  
  @SuppressWarnings("unused") 
  public static void verboseTest() {
    seq = VersionedSlotFactory.prototype.newSequence(~2);
    Version v0 = Version.getVersion();
    printSeq("initially");
    seq.setElementAt(new Integer(23),1);
    Version v1 = Version.getVersion();
    printSeq("after setting [1] = 23");
    seq.appendElement(new Integer(-4));
    Version v2 = Version.getVersion();
    printSeq("after adding [2] = -4");
    seq.appendElement("77");
    Version v22 = Version.getVersion();
    printSeq("after adding [3] = 77");
    Version.setVersion(v1);
    printSeq("after undoing both appends");
    seq.insertElement("new");
    Version v3 = Version.getVersion();
    printSeq("then doing insert");
    seq.insertElementAfter("newer",seq.location(0));
    Version v4 = Version.getVersion();
    printSeq("after inserting 'newer'");
    seq.insertElementAfter("newest",seq.location(3));
    Version v5 = Version.getVersion();
    printSeq("after inserting 'newest'");
  }
}
