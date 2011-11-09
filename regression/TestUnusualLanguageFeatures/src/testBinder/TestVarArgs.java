package testBinder;

public class TestVarArgs {
	static void foo(Class... classes) {
		// Nothing to do
	}
	static void main() {
		foo(TestVarArgs.class);
	}
}
