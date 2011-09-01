package testParser.annotation;

public class TestUseOfAnno {
	Anno anno;
	
	int test() {
		return anno.foo() + anno.bar();
	}
}
