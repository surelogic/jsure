package test.iface.subInterfaces.explicit;

import com.surelogic.Mutable;
import com.surelogic.Immutable;

// Mutable
interface AA {}

// Mutable
interface BB extends AA {}

@Mutable
interface BB2 extends AA {}

// Mutable
interface CC extends BB2 {}

@Mutable 
interface CC2 extends BB2 {}

@Immutable
interface DD extends CC {}

@Immutable
interface DD2 extends CC2 {}
