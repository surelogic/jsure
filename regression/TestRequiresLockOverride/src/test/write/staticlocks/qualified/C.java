package test.write.staticlocks.qualified;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

@Regions({
	@Region("public static R"),
	@Region("public static Q"),
	@Region("public static P")
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
	@RegionLock("N is lock3 protects P")
})
public class C {
	public static final ReadWriteLock lock1 = new ReentrantReadWriteLock();
	public static final ReadWriteLock lock2 = new ReentrantReadWriteLock();
	public static final ReadWriteLock lock3 = new ReentrantReadWriteLock();
	
	
	@RequiresLock("test.write.staticlocks.qualified.C:L.writeLock(), test.write.staticlocks.qualified.C:M.writeLock()")
	public void m1(final C a, final C b, final C c) {}
	
	@RequiresLock("test.write.staticlocks.qualified.C:L.writeLock()")
	public void m2(final C a, final C b, final C c) {}
	
	// Explicit empty
	@RequiresLock("")
	public void m3(final C a) {}
	
	// Implicit empty
	public void m4(final C a) {}	
}



/* BASIC CHECKS:
 * (1) Can completely remove requirements
 * (2) @RequiresLock("") is equivalent to no @RequiresLock
 */
class Good1 extends C {
	// GOOD: Can remove requirements; use implicit empty
	@Override
	public void m1(final C a, final C b, final C c) {}
	
	// GOOD: Can remove requirements: use explicit empty
	@Override
	@RequiresLock("")
	public void m2(final C a, final C b, final C c) {}
	
	// GOOD: Same as explicit empty
	@Override
	public void m3(final C a) {}
	
	// GOOD: Same as implicit empty
	@Override
	@RequiresLock("")
	public void m4(final C a) {}
}



/* BASIC CHECKS: Cannot add requirements
 */
class Bad1 extends C {
	// BAD: Cannot add lock
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:L.writeLock(), test.write.staticlocks.qualified.C:M.writeLock(), test.write.staticlocks.qualified.C:N.writeLock()")
	public void m1(final C a, final C b, final C c) {}
	
	// BAD: Cannot add lock to explicit empty
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:L.writeLock()")
	public void m3(final C a) {}
	
	// BAD: Cannot add lock to implicit empty
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:M.writeLock()")
	public void m4(final C a) {}		
}



/* Can partially remove requirements */
class Good2a extends C {
	// GOOD: Can remove requirements;
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:L.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Good2n extends C {
	// GOOD: Can remove requirements;
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:L.readLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Good3a extends C {
	// GOOD: Can remove requirements
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:M.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}
class Good3b extends C {
	// GOOD: Can remove requirements
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:M.readLock()")
	public void m1(final C a, final C b, final C c) {}
}

class Bad2 extends C {
	// BAD: Completely different
	@Override
	@RequiresLock("test.write.staticlocks.qualified.C:N.writeLock()")
	public void m1(final C a, final C b, final C c) {}
}


// === Test against other ways of naming locks ===

/*
 * (1) parameter renaming (correct and wrong)
 * (2) implicit receiver
 * (3) explicit receiver
 * (4) 0th-qualified receiver
 * (6) static locks
 *
 * Also, correct name, but wrong lock
 * 
 * The above, cross read lock and write lock
 */

@Regions({
	@Region("public static R"),
	@Region("public static Q"),
	@Region("public static J"),
	@Region("public I"),
	@Region("public I_J")
})
@RegionLocks({
	@RegionLock("L is lock1 protects R"),
	@RegionLock("M is lock2 protects Q"),
	@RegionLock("LL is class protects J"),
	@RegionLock("N is rwLock protects I"),
	@RegionLock("NN is this protects I_J")
})
class X {
	public static final ReadWriteLock lock1 = new ReentrantReadWriteLock();
	public static final ReadWriteLock lock2 = new ReentrantReadWriteLock();
	public final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	@RequiresLock("test.write.staticlocks.qualified.X:L.writeLock()")
	public void m(final X a, final X b) {}
}

// ==== Intrinsic Locks ====

// FORMALS

class XBad1 extends X {
	// BAD: formal
	@Override
	@RequiresLock("a:NN")
	public void m(final X a, final X b) {}
}

class XBad2 extends X {
	// BAD: formal (renamed), correct l
	@Override
	@RequiresLock("aa:NN")
	public void m(final X aa, final X bb) {}
}

// RECEIVERS

class XBad3 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("NN")
	public void m(final X a, final X b) {}
}

class XBad4 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:NN")
	public void m(final X a, final X b) {}
}

class XBad5 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad5.this:NN")
	public void m(final X a, final X b) {}
}

// STATIC LOCK

class XBad6 extends X {
	// BAD: static lock -- implicit
	@Override
	@RequiresLock("LL")
	public void m(final X a, final X b) {}
}

class XBad7 extends X {
	// BAD: static lock -- explicit
	@Override
	@RequiresLock("test.write.staticlocks.qualified.X:LL")
	public void m(final X a, final X b) {}
}

class XBad8 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.write.staticlocks.qualified.XBad8:LL")
	public void m(final X a, final X b) {}
}

// ==== Read Locks ====

// FORMALS

class XBad9 extends X {
	// BAD: formal
	@Override
	@RequiresLock("a:N.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad10 extends X {
	// BAD: formal (renamed), correct l
	@Override
	@RequiresLock("aa:N.readLock()")
	public void m(final X aa, final X bb) {
	}
}

// RECEIVERS

class XBad11 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("N.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad12 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:N.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad13 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad13.this:N.readLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XGood1 extends X {
	// GOOD: static lock -- implicit; can decrease write to read
	@Override
	@RequiresLock("L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XGood2 extends X {
	// GOOD: static lock -- explicit; can decrease write to read
	@Override
	@RequiresLock("test.write.staticlocks.qualified.X:L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XGood3 extends X {
	// GOOD: static lock -- explicit 2; can decrease write to read
	@Override
	@RequiresLock("test.write.staticlocks.qualified.XGood3:L.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad14 extends X {
	// BAD: wrong lock, static lock -- implicit; can decrease write to read
	@Override
	@RequiresLock("M.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad15 extends X {
	// BAD: wrong lock, static lock -- explicit; can decrease write to read
	@Override
	@RequiresLock("test.write.staticlocks.qualified.X:M.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad16 extends X {
	// BAD: wrong lock, static lock -- explicit 2; can decrease write to read
	@Override
	@RequiresLock("test.write.staticlocks.qualified.XBad16:M.readLock()")
	public void m(final X a, final X b) {
	}
}
// ==== Write Locks ====

// FORMALS

class XBad17 extends X {
	// BAD: formal
	@Override
	@RequiresLock("a:N.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad18 extends X {
	// BAD: formal (renamed), correct l
	@Override
	@RequiresLock("aa:N.writeLock()")
	public void m(final X aa, final X bb) {
	}
}

// RECEIVERS

class XBad19 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("N.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad20 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:N.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad21 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad21.this:N.writeLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XGood4 extends X {
	// GOOD: static lock -- implicit
	@Override
	@RequiresLock("L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XGood5 extends X {
	// GOOD: static lock -- explicit
	@Override
	@RequiresLock("test.write.staticlocks.qualified.X:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XGood6 extends X {
	// GOOD: static lock -- explicit 2
	@Override
	@RequiresLock("test.write.staticlocks.qualified.XGood6:L.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad22 extends X {
	// GOOD: static lock -- implicit, wrong lock
	@Override
	@RequiresLock("M.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad23 extends X {
	// GOOD: static lock -- explicit, wrong lock
	@Override
	@RequiresLock("test.write.staticlocks.qualified.X:M.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad24 extends X {
	// GOOD: static lock -- explicit 2, wrong lock
	@Override
	@RequiresLock("test.write.staticlocks.qualified.XBad24:M.writeLock()")
	public void m(final X a, final X b) {
	}
}
