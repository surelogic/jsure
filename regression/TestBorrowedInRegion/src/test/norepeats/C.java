package test.norepeats;

import com.surelogic.Borrowed;
import com.surelogic.BorrowedInRegion;


@SuppressWarnings("unused")
public final class C {
  // Good
  @Borrowed
  private final Object good1 = new Object();

  // Good
  @BorrowedInRegion("Instance")
  private final Object good2 = new Object();

  // Good
  @BorrowedInRegion("Instance into Instance")
  private final Object good3 = new Object();
  
  // Bad
  @Borrowed
  @BorrowedInRegion("Instance")
  private final Object bad1 = new Object();
  
  // Bad
  @Borrowed
  @BorrowedInRegion("Instance into Instance")
  private final Object bad2 = new Object();

  // Not possible
//  @@BorrowedInRegion("Instance")
//  @@BorrowedInRegion("Instance into Instance")
//  private final Object bad3 = new Object();
}
