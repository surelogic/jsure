package test;

import com.surelogic.ThreadSafe;

// BAD: Extends immutable class, must be immutable
@ThreadSafe
public class ThreadSafe2 extends ImmutableClass {

}
