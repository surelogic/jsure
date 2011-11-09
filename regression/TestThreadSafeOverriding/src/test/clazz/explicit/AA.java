package test.clazz.explicit;

import com.surelogic.NotThreadSafe;

// NotThreadSafe
class AA {}

// NotThreadSafe
class BB extends AA {}

@NotThreadSafe
class BB2 extends AA {}

class CC extends BB2 {}

@NotThreadSafe 
class CC2 extends BB2 {}

