package testRecognition.interfaces;

import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;

// GOOD
@ThreadSafe // implOnly = false, verify=true
public interface C {
}

// BAD: cannot be implementation only
@ThreadSafe(implementationOnly=true) // verify=true
interface D {
}

// GOOD
@ThreadSafe(implementationOnly=false) // verify=true
interface E {
}

// GOOD
@ThreadSafe(verify=true) // implOnly = false
interface F {
}

// BAD: cannot be verify=false
@ThreadSafe(verify=false) // implOnly = false
interface G {
}

// BAD: cannot be verify=false
@ThreadSafe(implementationOnly=false, verify=false)
interface H {
}

@ThreadSafe(implementationOnly=false, verify=true)
interface I {
}

// BAD: cannot be implementation only
@ThreadSafe(implementationOnly=true, verify=false)
interface J {
}

// BAD: cannot be implementation only
// BAD: cannot be verify=false
@ThreadSafe(implementationOnly=true, verify=true)
interface K {
}

@NotThreadSafe
interface Not {
}

// BAD: Cannot be both @NotThreadSafe and @ThreadSafe
@NotThreadSafe
@ThreadSafe
interface Both {
}
