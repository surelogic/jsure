package testRecognition.classes;

import com.surelogic.Containable;
import com.surelogic.NotContainable;
import com.surelogic.Unique;

@Containable // implOnly = false, verify=true
public class C {
  @Unique("return")
  public C() {}
}

@Containable(implementationOnly=true) // verify=true
class D {
  @Unique("return")
  public D() {}
}

@Containable(implementationOnly=false) // verify=true
class E {
  @Unique("return")
  public E() {}
}

@Containable(verify=true) // implOnly = false
class F {
  @Unique("return")
  public F() {}
}

@Containable(verify=false) // implOnly = false
class G {
  @Unique("return")
  public G() {}
}

@Containable(implementationOnly=false, verify=false)
class H {
  @Unique("return")
  public H() {}
}

@Containable(implementationOnly=false, verify=true)
class I {
  @Unique("return")
  public I() {}
}

@Containable(implementationOnly=true, verify=false)
class J {
  @Unique("return")
  public J() {}
}

@Containable(implementationOnly=true, verify=true)
class K {
  @Unique("return")
  public K() {}
}

@NotContainable
class Not {
  public Not() {}
}

// BAD: Cannot be both @NotContainable and @Containable
@NotContainable
@Containable
class Both1 {
  public Both1() {}
}

//BAD: Cannot be both @NotContainable and @Containable
@NotContainable
@Containable(implementationOnly=false)
class Both2 {
public Both2() {}
}

//GOOD: @NotContainable may coexist with @Containable(implementationOnly=true)
@NotContainable
@Containable(implementationOnly=true)
class Both3 {
	@Unique("return")
	public Both3() {}
}
