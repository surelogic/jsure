package testRecognition.interfaces;

import com.surelogic.Containable;
import com.surelogic.NotContainable;

// GOOD
@Containable // implOnly = false, verify=true
public interface C {
}

// BAD: cannot be implementation only
@Containable(implementationOnly=true) // verify=true
interface D {
}

// GOOD
@Containable(implementationOnly=false) // verify=true
interface E {
}

// GOOD
@Containable(verify=true) // implOnly = false
interface F {
}

// BAD: cannot be verify=false
@Containable(verify=false) // implOnly = false
interface G {
}

// BAD: cannot be verify=false
@Containable(implementationOnly=false, verify=false)
interface H {
}

@Containable(implementationOnly=false, verify=true)
interface I {
}

// BAD: cannot be implementation only
@Containable(implementationOnly=true, verify=false)
interface J {
}

// BAD: cannot be implementation only
// BAD: cannot be verify=false
@Containable(implementationOnly=true, verify=true)
interface K {
}

@NotContainable
interface Not {
}

// BAD: Cannot be both @NotContainable and @Containable
@NotContainable
@Containable
interface Both {
}
