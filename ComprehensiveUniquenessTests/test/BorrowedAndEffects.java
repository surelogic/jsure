package test;

/** 
 * Set up situations where the effects of the method
 * do indicate that the borrowed parameter could be accessed by both the
 * paramter and the field.
 */
public class BorrowedAndEffects {
  /** @unique */
  private Object uniqueField = null;
  
  /**
   * @borrowed this
   */
  public BorrowedAndEffects() {}

  /**
   * @unique v
   * @writes Instance
   */
  public void update(final Object v) {
    uniqueField = v;
  }
  
  
  
  /**
   * Good.
   * @borrowed p
   * @reads this.Instance
   */
  public void tricky(final Object p) {
//    @SuppressWarnings("unused")
    final Object o = this.uniqueField;
    // o is preseverd
  }
  
  /**
   * This method contains a bad call site because it passes "x" as 
   * the receiver and "x.uniqueField" as the parameter to method tricky().
   * This means that the method can access "x.uniqueField" through both "this.uniqueField"
   * and "p", and thus uniqueness is not maintained inside of the method.
   */
  public void containsBadBorrowedUsage() {
    // Create a new unqiue object
    final BorrowedAndEffects x = new BorrowedAndEffects();
    
    // bad call
    x.tricky(x.uniqueField);
  }
  
  /**
   * The call to tricky() here is good because the effects of the method call
   * do not conflict with the actual borrowed parameter.
   */
  public void containsGoodBorrowedUsage() {
    final BorrowedAndEffects y = new BorrowedAndEffects();
    final BorrowedAndEffects z = new BorrowedAndEffects();
    // good call
    y.tricky(z.uniqueField);
    
  }
}
