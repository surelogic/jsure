package test.borrowedReceiver;

import com.surelogic.Borrowed;

public class Root {
  // Not originally borrowed
  public void notOriginallyBorrowed() {}
  
  // Kept borrowed
  @Borrowed("this")
  public void keptBorrowed() {}
  
  // Not kept borrowed
  @Borrowed("this")
  public void notKeptBorrowed() {}
}

class Sub extends Root {
  // GOOD: can add 
  @Override
  @Borrowed("this")
  public void notOriginallyBorrowed() {}
  
  // GOOD: Still borrowed
  @Override
  @Borrowed("this")
  public void keptBorrowed() {}
  
  // BAD: Cannot remove
  @Override
  public void notKeptBorrowed() {}
}