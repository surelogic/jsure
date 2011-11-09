package test.borrowedParam;

import com.surelogic.Borrowed;

public class Root {
  // Not originally borrowed
  public void notOriginallyBorrowed1(Object p, Object q, Object r) {}
  
  // Not originally borrowed
  public void notOriginallyBorrowed2(Object p, Object q, Object r) {}
  
  // Not originally borrowed
  public void notOriginallyBorrowed3(Object p, Object q, Object r) {}
  
  // Kept borrowed
  public void keptBorrowed1(@Borrowed Object p, Object q, Object r) {}
  
  // Kept borrowed
  public void keptBorrowed2(Object p, @Borrowed Object q, Object r) {}
  
  // Kept borrowed
  public void keptBorrowed3(Object p, Object q, @Borrowed Object r) {}
  
  // Not kept borrowed
  public void notKeptBorrowed1(@Borrowed Object a) {}
}

class Sub extends Root {
  // GOOD: May add borrowed
  @Override
  public void notOriginallyBorrowed1(@Borrowed Object p, Object q, Object r) {}
  
  // GOOD: May add borrowed
  @Override
  public void notOriginallyBorrowed2(Object p, @Borrowed Object q, Object r) {}
  
  // GOOD: May add borrowed
  @Override
  public void notOriginallyBorrowed3(Object p, Object q, @Borrowed Object r) {}
  
  // GOOD: Kept borrowed
  @Override
  public void keptBorrowed1(@Borrowed Object p, Object q, Object r) {}
  
  // GOOD: Kept borrowed
  @Override
  public void keptBorrowed2(Object p, @Borrowed Object q, Object r) {}
  
  // GOOD: Kept borrowed
  @Override
  public void keptBorrowed3(Object p, Object q, @Borrowed Object r) {}
  
  // BAD: Not kept borrowed
  @Override
  public void notKeptBorrowed1(Object a) {}
}


class SubRenaming extends Root {
  // GOOD: May add borrowed
  @Override
  public void notOriginallyBorrowed1(@Borrowed Object x, Object y, Object z) {}
  
  // GOOD: May add borrowed
  @Override
  public void notOriginallyBorrowed2(Object x, @Borrowed Object y, Object z) {}
  
  // GOOD: May add borrowed
  @Override
  public void notOriginallyBorrowed3(Object x, Object y, @Borrowed Object z) {}
  
  // GOOD: Kept borrowed
  @Override
  public void keptBorrowed1(@Borrowed Object x, Object y, Object z) {}
  
  // GOOD: Kept borrowed
  @Override
  public void keptBorrowed2(Object x, @Borrowed Object y, Object z) {}
  
  // GOOD: Kept borrowed
  @Override
  public void keptBorrowed3(Object x, Object y, @Borrowed Object z) {}
  
  // BAD: Not kept borrowed
  @Override
  public void notKeptBorrowed1(Object t) {}
}
