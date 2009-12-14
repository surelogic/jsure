package test;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

/* Test calling methods and constructors that promise borrowed parameters.
 * Treat constructors and methods as separate cases.  Treat receiver
 * separately from regular parameters.  Test by passing both unique
 * and non unique actuals.
 */
public class BorrowedParams {
  /**
   * Good
   */
  @Unique("return")
  public BorrowedParams() {
    // Do nothing; preserves uniqueness of receiver
  }



  /**
   * Good.  Constructor that promises a borrowed parameter.
   */
  private BorrowedParams(final @Borrowed Object p) {
    // Do nothing; preserves state of p
  }



  /**
   * GOOD.  Return a unique reference.
   */
  @Unique("return")
  private BorrowedParams getUnique() {
    return new BorrowedParams();
  }

  /**
   * Good.  Return a possibly shared reference.
   */
  private BorrowedParams getShared() {
    return new BorrowedParams();
  }

  /**
   * Good.  Flag method for testing that parameter is still unique.
   */
  private static void stillUnique(final @Unique Object p) {
    // do nothing
  }

  /**
   * Good.  Flag method for test that parameter is still usable.
   */
  private static void stillShared(final Object p) {
    // do nothing
  }

  /**
   * Good.  Flag method that removes uniqueness from newly created objects
   * by not promising to borrow them.
   */
  private void removeUniqueness(final Object p) {
    // do nothing
  }

  /**
   * Good.
   */
  @Borrowed("this")
  private void borrowedReceiver() {
    // Do nothing; doesn't alias this
  }

  /**
   * Good
   */
  private static void borrowedParam(final @Borrowed Object p) {
    // Do nothing; doesn't alias this
  }


  /**
   * Good.  Unique newly created object preserved through call sequence.
   */
  public void goodUniqueBorrowedReceiver1() {
    final BorrowedParams o = new BorrowedParams();
    o.borrowedReceiver();
    stillUnique(o);
  }

  /**
   * Good.  Unique object preserved through call sequence.
   */
  public void goodUniqueBorrowedReceiver2() {
    final BorrowedParams o = getUnique();
    o.borrowedReceiver();
    stillUnique(o);
  }

  /**
   * Good.  Unique parameter preserved through call sequence.
   */
  public void goodUniqueBorrowedReceiver3(final @Unique BorrowedParams p) {
    p.borrowedReceiver();
    stillUnique(p);
  }

  /**
   * Good.  Unique receiver preserved through call sequence.
   */
  @Unique("this")
  public void goodUniqueBorrowedReceiver4() {
    this.borrowedReceiver();
    stillUnique(this);
  }



  /**
   * Good.  Unique newly created object preserved through call sequence.
   */
  public void goodUniqueBorrowedParam1() {
    final BorrowedParams o = new BorrowedParams();
    borrowedParam(o);
    stillUnique(o);
  }

  /**
   * Good.  Unique object preserved through call sequence.
   */
  public void goodUniqueBorrowedParam2() {
    final BorrowedParams o = getUnique();
    borrowedParam(o);
    stillUnique(o);
  }

  /**
   * Good.  Unique parameter preserved through call sequence.
   */
  public void goodUniqueBorrowedParam3(final @Unique BorrowedParams p) {
    borrowedParam(p);
    stillUnique(p);
  }

  /**
   * Good.  Unique receiver preserved through call sequence.
   */
  @Unique("this")
  public void goodUniqueBorrowedParam4() {
    borrowedParam(this);
    stillUnique(this);
  }



  /**
   * Good.  Unique newly created object preserved through call sequence.
   */
  public void goodUniqueBorrowedParam1b() {
    final BorrowedParams o = new BorrowedParams();
    new BorrowedParams(o);
    stillUnique(o);
  }

  /**
   * Good.  Unique object preserved through call sequence.
   */
  public void goodUniqueBorrowedParam2b() {
    final BorrowedParams o = getUnique();
    new BorrowedParams(o);
    stillUnique(o);
  }

  /**
   * Good.  Unique parameter preserved through call sequence.
   */
  public void goodUniqueBorrowedParam3b(final @Unique BorrowedParams p) {
    new BorrowedParams(p);
    stillUnique(p);
  }

  /**
   * Good.  Unique receiver preserved through call sequence.
   */
  @Unique("this")
  public void goodUniqueBorrowedParam4b() {
    new BorrowedParams(this);
    stillUnique(this);
  }



  /**
   * Good.  Shared newly created object preserved through call sequence.
   */
  public void goodSharedBorrowedReceiver1() {
    final BorrowedParams o = new BorrowedParams();
    removeUniqueness(o);
    o.borrowedReceiver();
    stillShared(o);
  }

  /**
   * Good.  Shared object preserved through call sequence.
   */
  public void goodSharedBorrowedReceiver2() {
    final BorrowedParams o = getShared();
    o.borrowedReceiver();
    stillShared(o);
  }

  /**
   * Good.  Shared parameter preserved through call sequence.
   */
  public void goodSharedBorrowedReceiver3(final BorrowedParams p) {
    p.borrowedReceiver();
    stillShared(p);
  }

  /**
   * Good.  Shared receiver preserved through call sequence.
   */
  public void goodSharedBorrowedReceiver4() {
    this.borrowedReceiver();
    stillShared(this);
  }



  /**
   * Good.  Shared newly created object preserved through call sequence.
   */
  public void goodSharedBorrowedParam1() {
    final BorrowedParams o = new BorrowedParams();
    removeUniqueness(o);
    borrowedParam(o);
    stillShared(o);
  }

  /**
   * Good.  Shared object preserved through call sequence.
   */
  public void goodSharedBorrowedParam2() {
    final BorrowedParams o = getShared();
    borrowedParam(o);
    stillShared(o);
  }

  /**
   * Good.  Shared parameter preserved through call sequence.
   */
  public void goodSharedBorrowedParam3(final BorrowedParams p) {
    borrowedParam(p);
    stillShared(p);
  }

  /**
   * Good.  Shared receiver preserved through call sequence.
   */
  public void goodSharedBorrowedParam4() {
    borrowedParam(this);
    stillShared(this);
  }



  /**
   * Good.  Shared newly created object preserved through call sequence.
   */
  public void goodSharedBorrowedParam1b() {
    final BorrowedParams o = new BorrowedParams();
    removeUniqueness(o);
    new BorrowedParams(o);
    stillShared(o);
  }

  /**
   * Good.  Shared object preserved through call sequence.
   */
  public void goodSharedBorrowedParam2b() {
    final BorrowedParams o = getShared();
    new BorrowedParams(o);
    stillShared(o);
  }

  /**
   * Good.  Shared parameter preserved through call sequence.
   */
  public void goodSharedBorrowedParam3b(final BorrowedParams p) {
    new BorrowedParams(p);
    stillShared(p);
  }

  /**
   * Good.  Shared receiver preserved through call sequence.
   */
  public void goodSharedBorrowedParam4b() {
    new BorrowedParams(this);
    stillShared(this);
  }
}
