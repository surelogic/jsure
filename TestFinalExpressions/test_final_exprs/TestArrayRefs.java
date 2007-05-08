package test_final_exprs;

/**
 * Tests the finality or not of array reference expressions.
 * @author aarong
 *
 */
public class TestArrayRefs {
  public void good_finalArray_constIdx() {
    final Object[] array = new Object[10];
    
    /* FINAL: array expression is final local variable; index is integer literal */
    synchronized (array[0]) {
      // do stuff
    }
  }

  public void good_finalArray_constIdx(final Object[] array) {
    /* FINAL: array expression is final parameter; index is integer literal */
    synchronized (array[0]) {
      // do stuff
    }
  }

  
  
  public void good_finalArray_finalIdx() {
    final Object[] array = new Object[10];
    final int idx = 5;
    
    /* FINAL: array expression is final local variable; index is final local */
    synchronized (array[idx]) {
      // do stuff
    }
  }

  public void good_finalArray_finalIdx(final Object[] array, final int idx) {
    /* FINAL: array expression is final parameter; index is final parameter */
    synchronized (array[idx]) {
      // do stuff
    }
  }
  
  
  
  public void good_1() {
    Object[] array = new Object[10];
    int idx = 5;
    
    /* FINAL: array expression is local variable; index is local; 
     * array, idx, and array elements are unchanged in the block
     */
    synchronized (array[idx]) {
      // do stuff
    }
  }
  
  public void good_2(Object[] array, int idx) {
    /* FINAL: array expression is parameter; index is parameter; 
     * array, idx, and array elements are unchanged in the block
     */
    synchronized (array[idx]) {
      // do stuff
    }
  }
  
  public void bad_1() {
    Object[] array = new Object[10];
    int idx = 5;
    
    /* NON-FINAL: array expression is local variable; index is local; 
     * array is changed
     * index is unchanged
     * array elements are unchanged
     */
    synchronized (array[idx]) {
      // do stuff
      array = new Object[5];
    }
  }
  
  public void bad_2() {
    Object[] array = new Object[10];
    int idx = 5;
    
    /* NON-FINAL: array expression is local variable; index is local; 
     * array is unchanged
     * index is changed
     * array elements are unchanged
     */
    synchronized (array[idx]) {
      // do stuff
      idx = 3;
    }
  }
  
  public void bad_3() {
    Object[] array = new Object[10];
    int idx = 5;
    
    /* NON-FINAL: array expression is local variable; index is local; 
     * array is unchanged
     * index is unchanged
     * array elements are changed
     */
    synchronized (array[idx]) {
      // do stuff
      array[0] = new Object();
    }
  }
}
