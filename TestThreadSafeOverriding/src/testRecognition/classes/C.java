package testRecognition.classes;

import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

@ThreadSafe // implOnly = false, verify=true
public class C {
  @Unique("return")
  public C() {}
}

@ThreadSafe(implementationOnly=true) // verify=true
class D {
  @Unique("return")
  public D() {}
}

@ThreadSafe(implementationOnly=false) // verify=true
class E {
  @Unique("return")
  public E() {}
}

@ThreadSafe(verify=true) // implOnly = false
class F {
  @Unique("return")
  public F() {}
}

@ThreadSafe(verify=false) // implOnly = false
class G {
  @Unique("return")
  public G() {}
}

@ThreadSafe(implementationOnly=false, verify=false)
class H {
  @Unique("return")
  public H() {}
}

@ThreadSafe(implementationOnly=false, verify=true)
class I {
  @Unique("return")
  public I() {}
}

@ThreadSafe(implementationOnly=true, verify=false)
class J {
  @Unique("return")
  public J() {}
}

@ThreadSafe(implementationOnly=true, verify=true)
class K {
  @Unique("return")
  public K() {}
}

@NotThreadSafe
class Not {
  public Not() {}
}

// BAD: Cannot be both @NotThreadSafe and @ThreadSafe
@NotThreadSafe
@ThreadSafe
class Both1 {
  public Both1() {}
}

//BAD: Cannot be both @NotThreadSafe and @ThreadSafe
@NotThreadSafe
@ThreadSafe(implementationOnly=false)
class Both2 {
public Both2() {}
}

//GOOD: @NotThreadSafe may coexist with @ThreadSafe(implementationOnly=true)
@NotThreadSafe
@ThreadSafe(implementationOnly=true)
class Both3 {
public Both3() {}
}

