package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

/** 
 * Set up situations where the effects of the method
 * do indicate that the borrowed parameter could be accessed by both the
 * parameter and the field.
 */
public class BorrowedAndEffects {
  private @Unique Object uniqueField = null;
  
  @Borrowed("this")
  public BorrowedAndEffects() {}

  @RegionEffects("writes Instance")
  public void update(final @Unique Object v) {
    uniqueField = v;
  }
  
  
  
  /**
   * Good
   */
  @RegionEffects("reads this:Instance")
  public void tricky(final @Borrowed Object p) {
//    @SuppressWarnings("unused")
    final Object o = this.uniqueField;
    // o is preserved
  }
  
  /**
   * This method contains a bad call site because it passes "x" as 
   * the receiver and "x.uniqueField" as the parameter to method tricky().
   * This means that the method can access "x.uniqueField" through both "this.uniqueField"
   * and "p", and thus uniqueness is not maintained inside of the method.
   */
  public void containsBadBorrowedUsage() {
    // Create a new unique object
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
