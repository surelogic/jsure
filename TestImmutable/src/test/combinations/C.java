package test.combinations;

import com.surelogic.Immutable;
import com.surelogic.Mutable;
import com.surelogic.NotThreadSafe;
import com.surelogic.ThreadSafe;

@Mutable
@NotThreadSafe
public class C {

}


@Mutable
@ThreadSafe
class D {
	
}

@Immutable
@NotThreadSafe
class E {
	
}

@Immutable
@ThreadSafe
class F {
	
}

