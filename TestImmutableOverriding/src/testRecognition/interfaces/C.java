package testRecognition.interfaces;

import com.surelogic.Immutable;
import com.surelogic.Mutable;

// GOOD
@Immutable // implOnly = false, verify=true
public interface C {
}

// BAD: cannot be implementation only
@Immutable(implementationOnly=true) // verify=true
interface D {
}

// GOOD
@Immutable(implementationOnly=false) // verify=true
interface E {
}

// GOOD
@Immutable(verify=true) // implOnly = false
interface F {
}

// BAD: cannot be verify=false
@Immutable(verify=false) // implOnly = false
interface G {
}

// BAD: cannot be verify=false
@Immutable(implementationOnly=false, verify=false)
interface H {
}

@Immutable(implementationOnly=false, verify=true)
interface I {
}

// BAD: cannot be implementation only
@Immutable(implementationOnly=true, verify=false)
interface J {
}

// BAD: cannot be implementation only
// BAD: cannot be verify=false
@Immutable(implementationOnly=true, verify=true)
interface K {
}

@Mutable
interface Not {
}

// BAD: Cannot be both @Mutable and @Immutable
@Mutable
@Immutable
interface Both {
}
