package unique;

public class TestUniqueConstructorJavaDoc {
  /**
   * @TestResult is UNPARSEABLE: Cannot name "this" on constructor
   * @Unique this
   */
  public TestUniqueConstructorJavaDoc() {
  }

  /**
   * @TestResult is UNPARSEABLE: Cannot name "return" on constructor
   * @Unique return
   */
  public TestUniqueConstructorJavaDoc(int x) {
  }

  /**
   * @TestResult is UNPARSEABLE: No such parameter as "p"
   * @Unique p
   */
  public TestUniqueConstructorJavaDoc(int x, int y) {
  }
  
  /**
   * @TestResult is CONSISTENT
   * @Unique o
   */
  public TestUniqueConstructorJavaDoc(Object o) {
  }
  
  /**
   * @TestResult is UNPARSEABLE: Cannot name "this" on constructor, Cannot name "return" on constructor
   * @Unique this, return
   */
  public TestUniqueConstructorJavaDoc(int x, int y, int z) {
  }
  
  /**
   * @TestResult is UNPARSEABLE: Cannot name "this" on constructor
   * @Unique o, this
   */
  public TestUniqueConstructorJavaDoc(Object o, Object p) {
  }
}
