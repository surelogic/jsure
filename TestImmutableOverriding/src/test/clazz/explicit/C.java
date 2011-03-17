package test.clazz.explicit;

import com.surelogic.Immutable;
import com.surelogic.Mutable;
import com.surelogic.Unique;

// GOOD
@Immutable(implementationOnly=true)
public class C {
  @Unique("return")
  public C() {}
}

// GOOD: Doesn't have to be anything
@Mutable
class D extends C {
  @Unique("return")
  public D() {}
}

// GOOD: stays containable
@Immutable(implementationOnly=true)
class E extends C {
  @Unique("return")
  public E() {}
}

// BAD: Super is not containable
@Immutable(implementationOnly=true)
class F extends D {
  @Unique("return")
  public F() {}
}



//GOOD: implOnly=false can extend implOnly=true
@Immutable
class W extends C {
  @Unique("return")
  public W() {}
}

//GOOD: implOnly=false can extend implOnly=true
@Immutable(implementationOnly=false)
class WW extends C {
  @Unique("return")
  public WW() {}
}

// Good: implOnly=false can extend implOnly=false
@Immutable
class X extends W {
  @Unique("return")
  public X() {}
}

//Good: implOnly=false can extend implOnly=false
@Immutable(implementationOnly=false)
class XX extends W {
  @Unique("return")
  public XX() {}
}

// BAD: implyOnly=true cannot extend implOnly=false
@Immutable(implementationOnly=true)
class Y extends X {
  @Unique("return")
  public Y() {}
}

//BAD: Super is not containable
@Immutable
class Z extends D {
  @Unique("return")
  public Z() {}
}

//BAD: Super is not containable
@Immutable(implementationOnly=false)
class ZZ extends D {
  @Unique("return")
  public ZZ() {}
}

// BAD: not containable at all
@Mutable
class Foo extends W {
  @Unique("return")
  public Foo() {}
}

