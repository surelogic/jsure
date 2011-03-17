package test.clazz.explicit;

import com.surelogic.Mutable;

// Mutable
class AA {}

// Mutable
class BB extends AA {}

@Mutable
class BB2 extends AA {}

class CC extends BB2 {}

@Mutable 
class CC2 extends BB2 {}

