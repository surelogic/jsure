package test.staticlocks.qualified;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.ReturnsLock;

@Regions({
	@Region("public static S"),
	@Region("public static T"),
	@Region("public A")
})
@RegionLocks({
	@RegionLock("L is class protects S"),
	@RegionLock("M is lock protects T"),
	@RegionLock("N is this protects A")
})
public class C {
  public static final Object lock = new Object();
  
  // GOOD: No ancestors
  @ReturnsLock("test.staticlocks.qualified.C:L")
  public Object get(final C a, final C b) {
    return C.class;
  }
  
  // GOOD: No explicit annotation, no ancestors
  public Object get2(final C a, final C b) {
    return null;
  }
}

class Bad1 extends C {
  // BAD: Ancestor is annotated
  @Override
  public Object get(final C a, final C b) {
    return null;
  }
}

class Good1 extends C {
  // GOOD: Still unannotated
  @Override
  public Object get2(final C a, final C b) {
    return null;
  }
}

class Good2 extends C {
  // GOOD: Can add annotation
  @Override
  @ReturnsLock("M")
  public Object get2(final C a, final C b) {
    return lock;
  }
}

class Good3a extends C {
  // GOOD: matched ancestor: implicitly qualified lock
  @Override
  @ReturnsLock("L")
  public Object get(final C a, final C b) {
    return C.class;
  }
}

class Good3b extends C { // *
  // GOOD: matched ancestor: explicitly qualified lock
  @Override
  @ReturnsLock("test.staticlocks.qualified.C:L")
  public Object get(final C x, final C y) {
    return C.class;
  }
}

class Good3C extends C { // *
  // GOOD: matched ancestor: explicitly qualified lock 2
  @Override
  @ReturnsLock("test.staticlocks.qualified.Good3C:L")
  public Object get(final C x, final C y) {
    return C.class;
  }
}

class Bad2a extends C {
  // BAD: Wrong lock
  @Override
  @ReturnsLock("M")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad2b extends C {
  // BAD: Wrong lock: qualified
  @Override
  @ReturnsLock("test.staticlocks.qualified.C:M")
  public Object get(final C x, final C y) {
    return lock;
  }
}
class Bad2c extends C {
  // BAD: Wrong lock: qualified
  @Override
  @ReturnsLock("test.staticlocks.qualified.Bad2c:M")
  public Object get(final C x, final C y) {
    return lock;
  }
}

class Bad3 extends C {
  // BAD: lock from a parameter
  @Override
  @ReturnsLock("b:N")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad4a extends C {
  // BAD: the receiver (implicit)
  @Override
  @ReturnsLock("N")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad4b extends C {
  // BAD: the receiver (explicit)
  @Override
  @ReturnsLock("this:N")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad4c extends C {
  // BAD: the receiver (qualified)
  @Override
  @ReturnsLock("Bad4c.this:N")
  public Object get(final C a, final C b) {
    return lock;
  }
}
