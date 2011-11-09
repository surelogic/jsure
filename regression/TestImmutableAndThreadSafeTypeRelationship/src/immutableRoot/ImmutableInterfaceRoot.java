package immutableRoot;

import com.surelogic.Immutable;
import com.surelogic.ThreadSafe;

// GOOD: The root
@Immutable
public interface ImmutableInterfaceRoot {}

// BAD
@ThreadSafe
interface ThreadSafeInterfaceExtension extends ImmutableInterfaceRoot {}

// GOOD
@Immutable
interface ImmutableInterfaceExtension extends ImmutableInterfaceRoot {}

// BAD
@ThreadSafe
class ThreadSafeImplementation implements ImmutableInterfaceRoot {}

// GOOD
@Immutable
class ImmutableImplementation implements ImmutableInterfaceRoot {}



//GOOD: The Root
@Immutable(implementationOnly=true)
class ImmutableImplOnly {}

//GOOD
@ThreadSafe
class ThreadSafeExtension extends ImmutableImplOnly {}

//GOOD
@ThreadSafe(implementationOnly=true)
class ThreadSafeExtension2 extends ImmutableImplOnly {}

//GOOD
@Immutable
class ImmutableExtension extends ImmutableImplOnly {}

//GOOD
@Immutable(implementationOnly=true)
class ImmutableExtension2 extends ImmutableImplOnly {}



//GOOD: The Root
@Immutable
class ImmutableClass {}

//BAD
@ThreadSafe
class ThreadSafeExtension3 extends ImmutableClass {}

//BAD
@ThreadSafe(implementationOnly=true)
class ThreadSafeExtension4 extends ImmutableClass {}

//GOOD
@Immutable
class ImmutableExtension3 extends ImmutableClass {}

//BAD
@Immutable(implementationOnly=true)
class ImmutableExtension4 extends ImmutableClass {}



