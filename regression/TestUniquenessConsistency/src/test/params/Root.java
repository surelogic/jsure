package test.params;

import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.ReadOnly;
import com.surelogic.Unique;

public class Root {
	public void unique(@Unique Object p) {}
	public void allowRead(@Unique(allowRead=true) Object p) {}
	public void immutable(@Immutable Object p) {}
	public void shared(Object p) {}
	public void readOnly(@ReadOnly Object p) {}
	public void borrowed(@Borrowed Object p) {}
}

class BorrowedParam extends Root {
	// GOOD
	public void unique(@Borrowed Object p) {}
	// GOOD
	public void allowRead(@Borrowed Object p) {}
	// GOOD
	public void immutable(@Borrowed Object p) {}
	// GOOD
	public void shared(@Borrowed Object p) {}
	// GOOD
	public void readOnly(@Borrowed Object p) {}
	// GOOD
	public void borrowed(@Borrowed Object p) {}
}

class ReadOnlyParam extends Root {
	// GOOD
	public void unique(@ReadOnly Object p) {}
	// GOOD
	public void allowRead(@ReadOnly Object p) {}
	// GOOD
	public void immutable(@ReadOnly Object p) {}
	// GOOD
	public void shared(@ReadOnly Object p) {}
	// GOOD
	public void readOnly(@ReadOnly Object p) {}
	// BAD
	public void borrowed(@ReadOnly Object p) {}
}

class ImmutableParam extends Root {
	// GOOD
	public void unique(@Immutable Object p) {}
	// GOOD
	public void allowRead(@Immutable Object p) {}
	// GOOD
	public void immutable(@Immutable Object p) {}
	// BAD
	public void shared(@Immutable Object p) {}
	// BAD
	public void readOnly(@Immutable Object p) {}
	// BAD
	public void borrowed(@Immutable Object p) {}
}

class SharedParam extends Root {
	// GOOD
	public void unique(Object p) {}
	// GOOD
	public void allowRead(Object p) {}
	// BAD
	public void immutable(Object p) {}
	// GOOD
	public void shared(Object p) {}
	// BAD
	public void readOnly(Object p) {}
	// BAD
	public void borrowed(Object p) {}
}

class AllowReadParam extends Root {
	// GOOD
	public void unique(@Unique(allowRead=true) Object p) {}
	// GOOD
	public void allowRead(@Unique(allowRead=true) Object p) {}
	// BAD
	public void immutable(@Unique(allowRead=true) Object p) {}
	// BAD
	public void shared(@Unique(allowRead=true) Object p) {}
	// BAD
	public void readOnly(@Unique(allowRead=true) Object p) {}
	// BAD
	public void borrowed(@Unique(allowRead=true) Object p) {}
}

class UniqueParam extends Root {
	// GOOD
	public void unique(@Unique Object p) {}
	// BAD
	public void allowRead(@Unique Object p) {}
	// BAD
	public void immutable(@Unique Object p) {}
	// BAD
	public void shared(@Unique Object p) {}
	// BAD
	public void readOnly(@Unique Object p) {}
	// BAD
	public void borrowed(@Unique Object p) {}
}
