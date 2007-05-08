/*
 * Created on Feb 9, 2005
 */
package test_array_length;

/**
 * Test that the length "field" of arrays is not considered mutable data
 * by the lock analysis.
 * 
 * @author Edwin
 * @lock L is this protects Instance
 */
public class Test {
  /**
   * @unshared
   * @aggregate Instance into Instance
   */
  private final String[] exec = null;

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
      // GOOD: Access to exec[i] proteced by synchronized method
      com.append(exec[i] + " ");
    }
  }
}
