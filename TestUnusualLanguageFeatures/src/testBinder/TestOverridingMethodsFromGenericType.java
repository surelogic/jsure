package testBinder;

class Generic<T> {
	void foo(T t) {
		// Nothing to do
	}
}

public class TestOverridingMethodsFromGenericType extends Generic<Object> {
	@Override
	void foo(Object t) {
		// Nothing to do
	}
}
