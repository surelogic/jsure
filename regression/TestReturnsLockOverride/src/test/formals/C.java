package test.formals;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.ReturnsLock;

@Regions({
	@Region("public R"),
	@Region("public Q"),
	@Region("public static S")
})
@RegionLocks({
	@RegionLock("L is this protects R"),
	@RegionLock("M is lock protects Q"),
	@RegionLock("N is class protects S")
})
public class C {
  public final Object lock = new Object();
  
  // GOOD: No ancestors
  @ReturnsLock("a:L")
  public Object get(final C a, final C b) {
    return a;
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
  @ReturnsLock("b:L")
  public Object get2(final C a, final C b) {
    return b;
  }
}

class Good3a extends C {
  // GOOD: matched ancestor: same parameter name
  @Override
  @ReturnsLock("a:L")
  public Object get(final C a, final C b) {
    return a;
  }
}

class Good3b extends C {
  // GOOD: matched ancestor: different parameter name
  @Override
  @ReturnsLock("x:L")
  public Object get(final C x, final C y) {
    return x;
  }
}

class Bad2a extends C {
  // BAD: Wrong lock: same parameter name
  @Override
  @ReturnsLock("a:M")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad2b extends C {
  // BAD: Wrong lock: different parameter name
  @Override
  @ReturnsLock("x:M")
  public Object get(final C x, final C y) {
    return lock;
  }
}

class Bad3 extends C {
  // BAD: Wrong parameter
  @Override
  @ReturnsLock("b:L")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad4a extends C {
  // BAD: Wrong parameter: the receiver (implicit)
  @Override
  @ReturnsLock("L")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad4b extends C {
  // BAD: Wrong parameter: the receiver (explicit)
  @Override
  @ReturnsLock("this:L")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad4c extends C {
  // BAD: Wrong parameter: the receiver (qualified)
  @Override
  @ReturnsLock("Bad4c.this:L")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad5a extends C {
  // BAD: wrong lock: static (implicit)
  @Override
  @ReturnsLock("N")
  public Object get(final C a, final C b) {
    return lock;
  }
}

class Bad5b extends C {
  // BAD: wrong lock: static (qualified)
  @Override
  @ReturnsLock("test.formals.C:N")
  public Object get(final C a, final C b) {
    return lock;
  }
}
