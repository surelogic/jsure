/*
 * Created on Feb 9, 2005
 */
package test_array_length;

import com.surelogic.Aggregate;
import com.surelogic.RegionLock;
import com.surelogic.Unique;

/**
 * Test that the length "field" of arrays is not considered mutable data
 * by the lock analysis.
 */
@RegionLock("L is this protects Instance")
public class Test {
  private final @Unique @Aggregate("Instance into Instance") 
  String[] exec = null;

//  @SuppressWarnings("unused")
  private int getLength() {
    // GOOD: access to length of array doesn't need protection
    // GOOD: access to exec doesn't need protected because it is final
    return exec.length;
  }
  
//  @SuppressWarnings("unused")
  private synchronized void test() {
    StringBuffer com = new StringBuffer();
    com.append(exec.length);
    for (int i = 0; i < exec.length; i++) {
      // GOOD: Access to exec[i] protected by synchronized method
      com.append(exec[i] + " ");
    }
  }
}
