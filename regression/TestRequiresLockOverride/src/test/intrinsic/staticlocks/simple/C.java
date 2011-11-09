package test.intrinsic.staticlocks.simple;

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
	@RegionLock("L is class protects R"),
	@RegionLock("M is lock protects Q"),
	@RegionLock("N is lock2 protects P")
})
public class C {
	public static final Object lock = new Object();
	public static final Object lock2 = new Object();
	
	
	@RequiresLock("L, M")
	public void m1(final C a, final C b, final C c) {}
	
	@RequiresLock("L")
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
	@RequiresLock("L, M, N")
	public void m1(final C a, final C b, final C c) {}
	
	// BAD: Cannot add lock to explicit empty
	@Override
	@RequiresLock("L")
	public void m3(final C a) {}
	
	// BAD: Cannot add lock to implicit empty
	@Override
	@RequiresLock("M")
	public void m4(final C a) {}		
}



/* Can partially remove requirements */
class Good2 extends C {
	// GOOD: Can remove requirements;
	@Override
	@RequiresLock("L")
	public void m1(final C a, final C b, final C c) {}
}

class Good3 extends C {
	// GOOD: Can remove requirements
	@Override
	@RequiresLock("M")
	public void m1(final C a, final C b, final C c) {}
}

class Bad2 extends C {
	// BAD: Completely different
	@Override
	@RequiresLock("N")
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
	@Region("public static RW"),
	@Region("public I"),
	@Region("public I_RW")
})
@RegionLocks({
	@RegionLock("L is class protects R"),
	@RegionLock("M is lock protects Q"),
	@RegionLock("LL is rwLock protects RW"),
	@RegionLock("N is this protects I"),
	@RegionLock("NN is instanceRWLock protects I_RW")
})
class X {
	public static final Object lock = new Object();
	public static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	public final ReadWriteLock instanceRWLock = new ReentrantReadWriteLock();
	
	@RequiresLock("L")
	public void m(final X a, final X b) {}
}

// ==== Intrinsic Locks ====

// FORMALS

class XBad1 extends X {
	// BAD: formal
	@Override
	@RequiresLock("a:N")
	public void m(final X a, final X b) {}
}

class XBad2 extends X {
	// BAD: formal (renamed), correct l
	@Override
	@RequiresLock("aa:N")
	public void m(final X aa, final X bb) {}
}

// RECEIVERS

class XBad3 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("N")
	public void m(final X a, final X b) {}
}

class XBad4 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:N")
	public void m(final X a, final X b) {}
}

class XBad5 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad5.this:N")
	public void m(final X a, final X b) {}
}

// STATIC LOCK

class XGood1 extends X {
	// GOOD: static lock -- implicit
	@Override
	@RequiresLock("L")
	public void m(final X a, final X b) {}
}

class XGood2 extends X {
	// GOOD: static lock -- explicit
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.X:L")
	public void m(final X a, final X b) {}
}

class XGood3 extends X {
	// GOOD: static lock -- explicit 2
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.XGood3:L")
	public void m(final X a, final X b) {}
}

class XBad6 extends X {
	// BAD: static lock -- implicit, wrong lock
	@Override
	@RequiresLock("M")
	public void m(final X a, final X b) {}
}

class XBad7 extends X {
	// Good: static lock -- explicit
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.X:M")
	public void m(final X a, final X b) {}
}

class XBad8 extends X {
	// BAD: static lock -- explicit 2
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.XBad8:M")
	public void m(final X a, final X b) {}
}

// ==== Read Locks ====

// FORMALS

class XBad9 extends X {
	// BAD: formal
	@Override
	@RequiresLock("a:NN.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad10 extends X {
	// BAD: formal (renamed), correct l
	@Override
	@RequiresLock("aa:NN.readLock()")
	public void m(final X aa, final X bb) {
	}
}

// RECEIVERS

class XBad11 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("NN.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad12 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:NN.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad13 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad13.this:NN.readLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XBad14 extends X {
	// GOOD: static lock -- implicit
	@Override
	@RequiresLock("LL.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad15 extends X {
	// GOOD: static lock -- explicit
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.X:LL.readLock()")
	public void m(final X a, final X b) {
	}
}

class XBad16 extends X {
	// GOOD: static lock -- explicit 2
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.XBad16:LL.readLock()")
	public void m(final X a, final X b) {
	}
}

// ==== Write Locks ====

// FORMALS

class XBad17 extends X {
	// BAD: formal
	@Override
	@RequiresLock("a:NN.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad18 extends X {
	// BAD: formal (renamed), correct l
	@Override
	@RequiresLock("aa:NN.writeLock()")
	public void m(final X aa, final X bb) {
	}
}

// RECEIVERS

class XBad19 extends X {
	// BAD: implicit receiver
	@Override
	@RequiresLock("NN.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad20 extends X {
	// BAD: explicit receiver
	@Override
	@RequiresLock("this:NN.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad21 extends X {
	// BAD: 0th-qualified receiver
	@Override
	@RequiresLock("XBad21.this:NN.writeLock()")
	public void m(final X a, final X b) {
	}
}

// STATIC LOCK

class XBad22 extends X {
	// GOOD: static lock -- implicit
	@Override
	@RequiresLock("LL.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad23 extends X {
	// GOOD: static lock -- explicit
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.X:LL.writeLock()")
	public void m(final X a, final X b) {
	}
}

class XBad24 extends X {
	// GOOD: static lock -- explicit 2
	@Override
	@RequiresLock("test.intrinsic.staticlocks.simple.XBad24:LL.writeLock()")
	public void m(final X a, final X b) {
	}
}
