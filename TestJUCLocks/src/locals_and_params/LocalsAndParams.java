package locals_and_params;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;

/**
 * Test that analysis can handle lock fields referenced through local 
 * variables/method arguments.  This used to fail because the flow analysis
 * thought local could be null.  This has been fixed by using a very simple
 * intraprocedural isNull analysis. (Bug 709)
 */
@RegionLock("L is lockField protects data" /* is CONSISTENT */)
public class LocalsAndParams {
  private final Lock lockField = new ReentrantLock();
  private int data;

  
  
  /**
   * Test locals
   */
  public static void testLocals() {
    final LocalsAndParams one = new LocalsAndParams();
    one.lockField.lock();  // +
    try {
      // PROTECTED
      one.data = 10;
    } finally {
      one.lockField.unlock(); // +
    }
  }
  
  
  
  /**
   * Test parameters 
   */
  public int testParams(final LocalsAndParams other) {
    lockField.lock(); // *
    try {
      other.lockField.lock(); // **
      try {
        // PROTECTED
        return this.data +
        // PROTECTED
          other.data;
      } finally {
        other.lockField.unlock(); // **
      }
    } finally {
      lockField.unlock(); // *
    }
  }
}
