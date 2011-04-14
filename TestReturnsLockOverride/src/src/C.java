package src;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.ReturnsLock;

@RegionLock("L is this protects R")
@Region("public R")
public class C {
  // GOOD: No ancestors
  @ReturnsLock("L")
  public Object get() {
    return this;
  }
  
  public Object get2() {
    return null;
  }
}

class Bad extends C {
  // BAD: Ancestor is annotated
  @Override
  public Object get() {
    return null;
  }
  
  // GOOD: Still unannotated
  @Override
  public Object get2() {
    return null;
  }
}

class Good extends C {
  // GOOD: matched ancestor
  @Override
  @ReturnsLock("L")
  public Object get() {
    System.out.println("Foo");
    return this;
  }
  
  // GOOD: Can add annotation
  @Override
  @ReturnsLock("L")
  public Object get2() {
    return this;
  }
}

@RegionLock("M is lock protects Q")
@Region("public Q")
class Bad2 extends C {
  private final Object lock = new Object();
  
  // BAD: Does not match ancestor
  @Override
  @ReturnsLock("M")
  public Object get() {
    return lock;
  }
  
  // GOOD: Can add annotation
  @Override
  @ReturnsLock("L")
  public Object get2() {
    return this;
  }
  
  // GOOD: new method
  @ReturnsLock("M")
  public Object get3() {
    return lock;
  }
}
