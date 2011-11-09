package borrowed;

import com.surelogic.Borrowed;

public class BorrowedStaticMethod {
	 @Borrowed("this" /* is CONSISTENT */)
	 public void foo() {}

	 /* This makes no sense because static methods don't have receivers */
	 @Borrowed("this" /* is UNPARSEABLE */)
	 public static void bar() {}
}
