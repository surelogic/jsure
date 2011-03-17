package test.clazz.explicit;

import com.surelogic.NotContainable;

// NotContainable
class AA {}

// NotContainable
class BB extends AA {}

@NotContainable
class BB2 extends AA {}

class CC extends BB2 {}

@NotContainable 
class CC2 extends BB2 {}

