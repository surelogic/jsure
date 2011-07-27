package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Test {
  /** Unique reference to an Other object */
  private @Unique Other u;
  
  /** Shared reference to an Other object */
  private Other s;
  
  
  
  // ======================================================================
  // == Callees: All combinations of shared, unique, and borrowed for
  // ==          two parameters.
  // ======================================================================

  @RegionEffects("reads shared1:Instance, shared2:Instance")
  public static void sharedShared(final Other shared1, final Other shared2) {
    // foo
  }

  @RegionEffects("reads shared1:Instance, borrowed2:Instance")
  public static void sharedBorrowed(final Other shared1, final @Borrowed Other borrowed2) {
    // foo
  }

  @RegionEffects("reads shared1:Instance, unique2:Instance")
  public static void sharedUnique(final Other shared1, final @Unique Other unique2) {
    // foo
  }
  

  
  @RegionEffects("reads borrowed1:Instance, shared2:Instance")
  public static void borrowedShared(final @Borrowed Other borrowed1, final Other shared2) {
    // foo
  }
  
  @RegionEffects("reads borrowed1:Instance, borrowed2:Instance")
  public static void borrowedBorrowed(final @Borrowed Other borrowed1, final @Borrowed Other borrowed2) {
    // foo
  }
  
  @RegionEffects("reads borrowed1:Instance, unique2:Instance")
  public static void borrowedUnique(final @Borrowed Other borrowed1, final @Unique Other unique2) {
    // foo
  }
  

  
  @RegionEffects("reads unique1:Instance, shared2:Instance")
  public static void uniqueShared(final @Unique Other unique1, final Other shared2) {
    // foo
  }
  
  @RegionEffects("reads unique1:Instance, borrowed2:Instance")
  public static void uniqueBorrowed(final @Unique Other unique1, final @Borrowed Other borrowed2) {
    // foo
  }
  
  @RegionEffects("reads unique1:Instance, unique2:Instance")
  public static void uniqueUnique(final @Unique Other unique1, final @Unique Other unique2) {
    // foo
  }
  
  
  
  // ======================================================================
  // == Callers
  // ======================================================================
  
  // ------------------------------------------------------------
  // -- Repetition of shared reference
  // ------------------------------------------------------------
  
  public void c1() {
    // Good
    sharedShared(this.s, this.s);
  }
  
  public void c2() {
    // Good
    sharedBorrowed(this.s, this.s);
  }
  
  public void c3() {
    /* BAD: Cannot pass shared ref to the second unique parameter. Two errors:
     * (1) in opUndefine: The unique parameter is not satisfied, but then
     * analysis proceed anyway and makes this.s undefined, causing (2) in
     * opCompromiseNoRelease, the first parameter to be undefined too.
     */
    sharedUnique(this.s, this.s);
  }
  
  public void c4() {
    // Good
    borrowedShared(this.s, this.s);
  }
  
  public void c5() {
    // Good
    borrowedBorrowed(this.s, this.s);
  }
  
  public void c6() {
    /* BAD: Cannot pass shared ref to the second unique parameter. Two errors:
     * (1) in opUndefine: The unique parameter is not satisfied, but then
     * analysis proceed anyway and makes this.s undefined, causing (2) in
     * opBorrow, the first parameter to be undefined too.
     */
    borrowedUnique(this.s, this.s);
  }
  
  public void c7() {
    /* BAD: Cannot pass shared ref to the first unique parameter.  Error
     * in opUndefine.
     */
    uniqueShared(this.s, this.s);
  }
  
  public void c8() {
    /* BAD: Cannot pass shared ref to the first unique parameter.  Error
     * in opUndefine.
     */
    uniqueBorrowed(this.s, this.s);
  }
  
  public void c9() {
    /* BAD: Cannot pass shared ref to the second unique parameter. Two errors:
     * (1) in opUndefine: The unique parameter is not satisfied, but then
     * analysis proceed anyway and makes this.s undefined, causing (2) in
     * opUndefine, the first parameter to be undefined too.
     */
    uniqueUnique(this.s, this.s);
  }
  
  // ------------------------------------------------------------
  // -- Repetition of borrowed reference
  // ------------------------------------------------------------
  
  public void b1(final @Borrowed Other b) {
    /* BAD: Cannot pass a borrowed ref to the second shared parameter.
     * opCompromiseNoRelease error is on the second actual argument.
     */
    sharedShared(b, b);
  }
  
  public void b2(final @Borrowed Other b) {
    /* BAD: Cannot pass a borrowed ref to the first shared parameter.
     * Error in opCompromiseNoRelease on the first actual argument.
     */
    sharedBorrowed(b, b);
  }
  
  public void b3(final @Borrowed Other b) {
    /* BAD: Cannot pass a borrowed ref to the second unique parameter.
     * Two errors: (1) opUndefine fails on the second parameter, but then
     * analysis proceeds with b as undefined, and we get (2) in opCompromiseNoRelease
     * an undefined error. 
     */
    sharedUnique(b, b);
  }
  
  public void b4(final @Borrowed Other b) {
    /* BAD: Cannot pass a borrowed ref to the second shared parameter.
     * opCompromiseNoRelease error is on the second actual argument.
     */
    borrowedShared(b, b);
  }
  
  public void b5(final @Borrowed Other b) {
    // GOOD: ?? is this right ??
    borrowedBorrowed(b, b);
  }
  
  public void b6(final @Borrowed Other b) {
    /* BAD: Cannot pass a borrowed ref to the second unique parameter.
     * Two errors: (1) opUndefine fails on the second parameter, but then
     * analysis proceeds with b as undefined, and we get (2) in opBorrow
     * an undefined error. 
     */
    borrowedUnique(b, b);
  }
  
  public void b7(final @Borrowed Other b) {
    /* BAD: Cannot pass borrowed ref to the second shared parameter.  Causes
     * an error in opBorrow, which then causes 'b' to be seen as shared in
     * the first actual argument causing an error in opUndefine.
     */
    uniqueShared(b, b);
  }
  
  public void b8(final @Borrowed Other b) {
    /* BAD: Cannot pass borrowed ref to the first unique parameter.  Causes
     * an error in opUndefine.
     */
    uniqueBorrowed(b, b);
  }
  
  public void b9(final @Borrowed Other b) {
    /* BAD: Cannot pass a borrowed ref to the first unique parameter.
     * Two errors: (1) opUndefine fails on the second parameter, but then
     * analysis proceeds with b as undefined, and we get (2) in opUndefine
     * an undefined error. 
     */
    uniqueUnique(b, b);
  }
  
  // ------------------------------------------------------------
  // -- Repetition of unique reference
  // ------------------------------------------------------------

  // .. Direct use of unique field
  
  public void u1() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opCompromiseNoRelease for the 
     * first parameter.
     */
    sharedShared(this.u, this.u);
  }
  
  public void u2() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opCompromiseNoRelease for the 
     * first parameter.
     */
    sharedBorrowed(this.u, this.u);
  }
  
  public void u3() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opCompromiseNoRelease for the 
     * first parameter.
     */
    sharedUnique(this.u, this.u);
  }
  
  public void u4() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opBorrowed for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the read of 'this.u' undefines the field, and it must be restored before
     * the end of the method.
     */
    borrowedShared(this.u, this.u);
  }
  
  public void u5() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opBorrowed for the 
     * first parameter.
     */
    borrowedBorrowed(this.u, this.u);
  }
  
  public void u6() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opBorrowed for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the read of 'this.u' undefines the field, and it must be restored before
     * the end of the method.
     */
    borrowedUnique(this.u, this.u);
  }
  
  public void u7() {
    /* BAD: Use of 'this.u' to the second shared parameter makes the first
     * use undefined.  So undefined error in opUndefine for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the read of 'this.u' undefines the field, and it must be restored before
     * the end of the method.
     */
    uniqueShared(this.u, this.u);
  }
  
  public void u8() {
    /* BAD: Use of 'this.u' to the second borrowed parameter makes the first
     * use undefined.  So undefined error in opUndefine for the 
     * first parameter.
     */
    uniqueBorrowed(this.u, this.u);
  }
  
  public void u9() {
    /* BAD: Use of 'this.u' to the second unique parameter makes the first
     * use undefined.  So undefined error in opUndefine for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the read of 'this.u' undefines the field, and it must be restored before
     * the end of the method.
     */
    uniqueUnique(this.u, this.u);
  }
  
  // .. Use via variable

  public void v1() {
    Other x = new Other();
    Other q = this.u;
    try {
      // GOOD
      sharedShared(q, q);
    } finally {
      this.u = x;
    }
  }
  
  public void v2() {
    Other x = new Other();
    Other q = this.u;
    try {
      // GOOD
      sharedBorrowed(q, q);
    } finally {
      this.u = x;
    }
  }
  
  public void v3() {
    Other q = this.u;
    /* BAD: Passing 'q' to the unique second parameter makes 'q' undefined, so
     * we get an undefined error in opCompromiseNoRelease for the first
     * parameter.
     */
    sharedUnique(q, q);
  }
  
  public void v4() {
    Other x = new Other();
    Other q = this.u;
    try {
      // GOOD
      borrowedShared(q, q);
    } finally {
      this.u = x;
    }
  }
  
  public void v5() {
    Other x = new Other();
    Other q = this.u;
    try {
      // GOOD
      borrowedBorrowed(q, q);
    } finally {
      this.u = x;
    }
  }
  
  public void v6() {
    Other q = this.u;
    /* BAD: Use of 'q' to the second shared parameter makes the first
     * use undefined.  So undefined error in opBorrowed for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the read of 'this.u' undefines the field, and it must be restored before
     * the end of the method.
     */
    borrowedUnique(q, q);
  }
  
  public void v7() {
    Other q = this.u;
    /* BAD: Use of 'q' to the second shared parameter makes the first
     * use shared.  So shared error in opUndefine for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the sharing of 'q' undefines the field, and it must be restored before
     * the end of the method.
     */
    uniqueShared(q, q);
  }
  
  public void v8() {
    Other x = new Other();
    Other q = this.u;
    try {
      // GOOD
      uniqueBorrowed(q, q);
    } finally {
      this.u = x;
    }
  }
  
  public void v9() {
    Other q = this.u;
    /* BAD: Use of 'q' to the second unique parameter makes the first
     * use undefined.  So undefined error in opUndefine for the 
     * first parameter.  Also has a lost reference error (from check) because
     * the passing of 'q' to the unique parameter 
     * undefines the field, and it must be restored before
     * the end of the method.
     */
    uniqueUnique(q, q);
  }
}



class Other {
  @Unique("return")
  @RegionEffects("none")
  public Other() {
    super();
  }
}

