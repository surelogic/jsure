package test.uniqueReceiver;

import com.surelogic.Unique;

public class Root {
  // Not originally unique
  public void notOriginallyUnique1() {}
  
  // Not originally unique
  @Unique("return")
  public Object notOriginallyUnique2() {
    return null;
  }
  
  // Kept unique
  @Unique("this")
  public void keptUnique1() {}
  
  // Kept unique
  @Unique("this, return")
  public Object keptUnique2() {
    return null;
  }
  
  // Not kept unique
  @Unique("this")
  public void notKeptUnique1() {}
  
  // Not kept unique
  @Unique("this, return")
  public Object notKeptUnique2() {
    return null;
  }
}

class Sub extends Root {
  // BAD: Cannot add unique
  @Override
  @Unique("this")
  public void notOriginallyUnique1() {}
  
  // BAD: Cannot add unique
  @Override
  @Unique("this, return")
  public Object notOriginallyUnique2() {
    return null;
  }
  
  // GOOD: Kept unique
  @Override
  @Unique("this")
  public void keptUnique1() {}
  
  // GOOD: Kept unique
  @Override
  @Unique("this, return")
  public Object keptUnique2() {
    return null;
  }
  
  // GOOD: Not kept unique
  @Override
  public void notKeptUnique1() {}
  
  // GOOD: Not kept unique
  @Override
  @Unique("return")
  public Object notKeptUnique2() {
    return null;
  }
}