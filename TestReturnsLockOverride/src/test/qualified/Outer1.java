package test.qualified;

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
public class Outer1 {
  public final Object lock = new Object();

  
  
  
  @Regions({
  	@Region("public R"),
  	@Region("public Q")
  })
  @RegionLocks({
  	@RegionLock("L is this protects R"),
  	@RegionLock("M is lock protects Q")
  })
  public class C {
    public final Object lock = new Object();
    
    // GOOD: No ancestors
    @ReturnsLock("Outer1.this:L")
    public Object get(final Outer1 a, final Outer1 b) {
      return Outer1.this;
    }
    
    // GOOD: No explicit annotation, no ancestors
    public Object get2(final Outer1 a, final Outer1 b) {
      return null;
    }
  }

  class Bad1 extends C {
    // BAD: Ancestor is annotated
    @Override
    public Object get(final Outer1 a, final Outer1 b) {
      return null;
    }
  }

  class Good1 extends C {
    // GOOD: Still unannotated
    @Override
    public Object get2(final Outer1 a, final Outer1 b) {
      return null;
    }
  }

  class Good2 extends C {
    // GOOD: Can add annotation
    @Override
    @ReturnsLock("b:L")
    public Object get2(final Outer1 a, final Outer1 b) {
      return b;
    }
  }

  class Good3 extends C { // *
    // GOOD: matched ancestor 
    @Override
    @ReturnsLock("Outer1.this:L")
    public Object get(final Outer1 a, final Outer1 b) {
      return Outer1.this;
    }
  }
  
  class Bad2a extends C {
    // BAD: implicit receiver -- same lock name, different lock
    @Override
    @ReturnsLock("L")
    public Object get(final Outer1 a, final Outer1 b) {
      return this;
    }
  }

  class Bad2b extends C {
    // BAD: explicit receiver -- same lock name, different lock
    @Override
    @ReturnsLock("this:L")
    public Object get(final Outer1 a, final Outer1 b) {
      return this;
    }
  }

  class Bad2c extends C {
    // BAD: explicit qualified receiver -- same lock name, different lock
    @Override
    @ReturnsLock("Bad2c.this:L")
    public Object get(final Outer1 a, final Outer1 b) {
      return this;
    }
  }

  class Bad3 extends C {
    // BAD: Wrong lock
    @Override
    @ReturnsLock("Outer1.this:M")
    public Object get(final Outer1 a, final Outer1 b) {
      return lock;
    }
  }

  class Bad4 extends C {
    // BAD: Wrong parameter
    @Override
    @ReturnsLock("b:L")
    public Object get(final Outer1 a, final Outer1 b) {
      return lock;
    }
  }

  class Bad5 extends C {
    // BAD: wrong lock: static (qualified)
    @Override
    @ReturnsLock("test.qualified.Outer1:N")
    public Object get(final Outer1 a, final Outer1 b) {
      return lock;
    }
  }
  
	@Region("public R")
	@RegionLock("L is this protects R")
  public class Outer2 {
    class Good3 extends C { // *
      // GOOD: matched ancestor 
      @Override
      @ReturnsLock("Outer1.this:L")
      public Object get(final Outer1 a, final Outer1 b) {
        return Outer1.this;
      }
    }
    
    class Bad6 extends C {
      // BAD: Wrong qualified receiver
      @Override
      @ReturnsLock("Outer2.this:L")
      public Object get(final Outer1 a, final Outer1 b) {
        return Outer2.this;
      }
    }
  }
}


class X extends Outer1.C {
	public X(final Outer1 o) {
		o.super();
	}
	
	// BUMMER!  Cannot do this!
  @Override
  @ReturnsLock("Outer1.this:L")
  public Object get(final Outer1 a, final Outer1 b) {
    return null;
  }}