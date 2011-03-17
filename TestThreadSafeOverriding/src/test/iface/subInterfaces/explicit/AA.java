package test.iface.subInterfaces.explicit;

import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;

// NotThreadSafe
interface AA {}

// NotThreadSafe
interface BB extends AA {}

@NotThreadSafe
interface BB2 extends AA {}

// NotThreadSafe
interface CC extends BB2 {}

@NotThreadSafe 
interface CC2 extends BB2 {}

@ThreadSafe
interface DD extends CC {}

@ThreadSafe
interface DD2 extends CC2 {}
