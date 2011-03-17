package testRecognition.classes;

import com.surelogic.Immutable;
import com.surelogic.Mutable;
import com.surelogic.Unique;

@Immutable // implOnly = false, verify=true
public class C {
  @Unique("return")
  public C() {}
}

@Immutable(implementationOnly=true) // verify=true
class D {
  @Unique("return")
  public D() {}
}

@Immutable(implementationOnly=false) // verify=true
class E {
  @Unique("return")
  public E() {}
}

@Immutable(verify=true) // implOnly = false
class F {
  @Unique("return")
  public F() {}
}

@Immutable(verify=false) // implOnly = false
class G {
  @Unique("return")
  public G() {}
}

@Immutable(implementationOnly=false, verify=false)
class H {
  @Unique("return")
  public H() {}
}

@Immutable(implementationOnly=false, verify=true)
class I {
  @Unique("return")
  public I() {}
}

@Immutable(implementationOnly=true, verify=false)
class J {
  @Unique("return")
  public J() {}
}

@Immutable(implementationOnly=true, verify=true)
class K {
  @Unique("return")
  public K() {}
}

@Mutable
class Not {
  public Not() {}
}

// BAD: Cannot be both @Mutable and @Immutable
@Mutable
@Immutable
class Both1 {
  public Both1() {}
}

//BAD: Cannot be both @Mutable and @Immutable
@Mutable
@Immutable(implementationOnly=false)
class Both2 {
public Both2() {}
}

//GOOD: @Mutable may coexist with @Immutable(implementationOnly=true)
@Mutable
@Immutable(implementationOnly=true)
class Both3 {
public Both3() {}
}
