package test.iface.subInterfaces.explicit;

import com.surelogic.NotContainable;
import com.surelogic.Containable;

// NotContainable
interface AA {}

// NotContainable
interface BB extends AA {}

@NotContainable
interface BB2 extends AA {}

// NotContainable
interface CC extends BB2 {}

@NotContainable 
interface CC2 extends BB2 {}

@Containable
interface DD extends CC {}

@Containable
interface DD2 extends CC2 {}
