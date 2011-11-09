package threadSafeRoot;

import com.surelogic.Immutable;
import com.surelogic.ThreadSafe;

// GOOD: The root
@ThreadSafe
public interface ThreadSafeInterfaceRoot {}

// GOOD
@ThreadSafe
interface ThreadSafeInterfaceExtension extends ThreadSafeInterfaceRoot {}

// GOOD
@Immutable
interface ImmutableInterfaceExtension extends ThreadSafeInterfaceRoot {}

// GOOD
@ThreadSafe
class ThreadSafeImplementation implements ThreadSafeInterfaceRoot {}

// GOOD
@Immutable
class ImmutableImplementation implements ThreadSafeInterfaceRoot {}



//GOOD: The Root
@ThreadSafe(implementationOnly=true)
class ThreadSafeImplOnly {}

//GOOD
@ThreadSafe
class ThreadSafeExtension extends ThreadSafeImplOnly {}

//GOOD
@ThreadSafe(implementationOnly=true)
class ThreadSafeExtension2 extends ThreadSafeImplOnly {}

//BAD
@Immutable
class ImmutableExtension extends ThreadSafeImplOnly {}

//BAD
@Immutable(implementationOnly=true)
class ImmutableExtension2 extends ThreadSafeImplOnly {}



//GOOD: The Root
@ThreadSafe
class ThreadSafeClass {}

//GOOD
@ThreadSafe
class ThreadSafeExtension3 extends ThreadSafeClass {}

//BAD
@ThreadSafe(implementationOnly=true)
class ThreadSafeExtension4 extends ThreadSafeClass {}

//BAD
@Immutable
class ImmutableExtension3 extends ThreadSafeClass {}

//BAD
@Immutable(implementationOnly=true)
class ImmutableExtension4 extends ThreadSafeClass {}



