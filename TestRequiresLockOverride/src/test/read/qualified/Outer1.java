package test.read.qualified;


import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

@Regions({
	@Region("public R"),
	@Region("public Q"),
	@Region("public static S")
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
	@RegionLock("N is slock protects S")
})
public class Outer1 {
  public final ReadWriteLock lock1 = new ReentrantReadWriteLock();
  public final ReadWriteLock lock2 = new ReentrantReadWriteLock();
  public static final ReadWriteLock slock = new ReentrantReadWriteLock();

  
  
  
  @Regions({
  	@Region("public R"),
  	@Region("public Q")
  })
  @RegionLocks({
  	@RegionLock("L is lock1 protects R"),
  	@RegionLock("M is lock2 protects Q")
  })
  public class C {
    public final ReadWriteLock lock1 = new ReentrantReadWriteLock();
    public final ReadWriteLock lock2 = new ReentrantReadWriteLock();
    
    // GOOD: No ancestors
    @RequiresLock("Outer1.this:L.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
    
    // GOOD: No explicit annotation, no ancestors
    public void get2(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad1 extends C {
    // BAD: Cannot add
    @Override
    @RequiresLock("Outer1.this:L.readLock()")
    public void get2(final Outer1 a, final Outer1 b) {
    }
  }

  class Good1 extends C {
    // GOOD: Can remove annotation
    @Override
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Good2a extends C {
    // GOOD: matched ancestor 
    @Override
    @RequiresLock("Outer1.this:L.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad2a extends C {
    // BAD: writeLock
    @Override
    @RequiresLock("Outer1.this:L.writeLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }
  
  class Bad2b extends C {
    // BAD: implicit receiver -- same lock name, different lock
    @Override
    @RequiresLock("L.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad2c extends C {
    // BAD: explicit receiver -- same lock name, different lock
    @Override
    @RequiresLock("this:L.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad2d extends C {
    // BAD: explicit qualified receiver -- same lock name, different lock
    @Override
    @RequiresLock("Bad2d.this:L.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad3 extends C {
    // BAD: Wrong lock
    @Override
    @RequiresLock("Outer1.this:M.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad4 extends C {
    // BAD: Wrong parameter
    @Override
    @RequiresLock("b:L.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }

  class Bad5 extends C {
    // BAD: wrong lock: static (qualified)
    @Override
    @RequiresLock("test.intrinsic.qualified.Outer1:N.readLock()")
    public void get(final Outer1 a, final Outer1 b) {
    }
  }
  
	@Region("public R")
	@RegionLock("L is this protects R")
  public class Outer2 {
    class Good3 extends C { // *
      // GOOD: matched ancestor 
      @Override
      @RequiresLock("Outer1.this:L.readLock()")
      public void get(final Outer1 a, final Outer1 b) {
      }
    }
    
    class Bad6 extends C {
      // BAD: Wrong qualified receiver
      @Override
      @RequiresLock("Outer2.this:L.readLock()")
      public void get(final Outer1 a, final Outer1 b) {
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
  @RequiresLock("Outer1.this:L.readLock()")
  public void get(final Outer1 a, final Outer1 b) {
  }}