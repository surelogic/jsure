package testParser;

public class TestInterfaceDetection {
	void foo(AbstractInterface i) {
		System.out.println(i.incr(0));
	}
	void bar(AbstractAnno a) {
		System.out.println(a.count());
	}
}
