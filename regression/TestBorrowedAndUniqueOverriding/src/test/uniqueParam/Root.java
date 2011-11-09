package test.uniqueParam;

import com.surelogic.Unique;

public class Root {
  // Not originally unique
  public void notOriginallyUnique1(Object p, Object q, Object r) {}
  
  // Not originally unique
  public void notOriginallyUnique2(Object p, Object q, Object r) {}
  
  // Not originally unique
  public void notOriginallyUnique3(Object p, Object q, Object r) {}
  
  // Kept unique
  public void keptUnique1(@Unique Object p, Object q, Object r) {}
  
  // Kept unique
  public void keptUnique2(Object p, @Unique Object q, Object r) {}
  
  // Kept unique
  public void keptUnique3(Object p, Object q, @Unique Object r) {}
  
  // Not kept unique
  public void notKeptUnique1(@Unique Object a) {}
}

class Sub extends Root {
  // BAD: Cannot add unique
  @Override
  public void notOriginallyUnique1(@Unique Object p, Object q, Object r) {}
  
  // BAD: Cannot add unique
  @Override
  public void notOriginallyUnique2(Object p, @Unique Object q, Object r) {}
  
  // BAD: Cannot add unique
  @Override
  public void notOriginallyUnique3(Object p, Object q, @Unique Object r) {}
  
  // GOOD: Kept unique
  @Override
  public void keptUnique1(@Unique Object p, Object q, Object r) {}
  
  // GOOD: Kept unique
  @Override
  public void keptUnique2(Object p, @Unique Object q, Object r) {}
  
  // GOOD: Kept unique
  @Override
  public void keptUnique3(Object p, Object q, @Unique Object r) {}
  
  // GOOD: Not kept unique
  @Override
  public void notKeptUnique1(Object a) {}
}


class SubRenaming extends Root {
  // BAD: Cannot add unique
  @Override
  public void notOriginallyUnique1(@Unique Object x, Object y, Object z) {}
  
  // BAD: Cannot add unique
  @Override
  public void notOriginallyUnique2(Object x, @Unique Object y, Object z) {}
  
  // BAD: Cannot add unique
  @Override
  public void notOriginallyUnique3(Object x, Object y, @Unique Object z) {}
  
  // GOOD: Kept unique
  @Override
  public void keptUnique1(@Unique Object x, Object y, Object z) {}
  
  // GOOD: Kept unique
  @Override
  public void keptUnique2(Object x, @Unique Object y, Object z) {}
  
  // GOOD: Kept unique
  @Override
  public void keptUnique3(Object x, Object y, @Unique Object z) {}
  
  // GOOD: Not kept unique
  @Override
  public void notKeptUnique1(Object t) {}
}
