package borrowed;

import com.surelogic.Borrowed;

public class TestBadArgs {
  public void method1(@Borrowed Object p) {}
  public void method2(@Borrowed(/* is CONSISTENT */) Object p) {}
  public void method3(@Borrowed(""/* is CONSISTENT */) Object p) {}
  public void method4(@Borrowed("this" /* is UNPARSEABLE */) Object p) {}
  public void method5(@Borrowed("return" /* is UNPARSEABLE */) Object p) {}
  public void method6(@Borrowed("return, this" /* is UNPARSEABLE */) Object p) {}
  public void method7(@Borrowed("this, return" /* is UNPARSEABLE */) Object p) {}
  public void method8(@Borrowed(" f, a, b" /* is UNPARSEABLE */) Object p) {}
  public void method3a(@Borrowed(value=""/* is CONSISTENT */) Object p) {}
  public void method4a(@Borrowed(value="this" /* is UNPARSEABLE */) Object p) {}
  public void method5a(@Borrowed(value="return" /* is UNPARSEABLE */) Object p) {}
  public void method6a(@Borrowed(value="return, this" /* is UNPARSEABLE */) Object p) {}
  public void method7a(@Borrowed(value="this, return" /* is UNPARSEABLE */) Object p) {}
  public void method8a(@Borrowed(value=" f, a, b" /* is UNPARSEABLE */) Object p) {}
  
  
  @Borrowed  // bad
  public void annotatedMethod1() {}

  @Borrowed(/* is UNPARSEABLE */)
  public void annotatedMethod2() {}

  @Borrowed(""/* is UNPARSEABLE */)
  public void annotatedMethod3() {}

  @Borrowed("this" /* is CONSISTENT */)
  public void annotatedMethod4() {}

  @Borrowed("return" /* is UNPARSEABLE */)
  public void annotatedMethod5() {}

  @Borrowed("return, this" /* is UNPARSEABLE */)
  public void annotatedMethod6() {}

  @Borrowed("this, return" /* is UNPARSEABLE */)
  public void annotatedMethod7() {}

  @Borrowed(" f, a, b" /* is UNPARSEABLE */)
  public void annotatedMethod8() {}

  @Borrowed(value=""/* is UNPARSEABLE */)
  public void annotatedMethod3a() {}

  @Borrowed(value="this" /* is CONSISTENT */)
  public void annotatedMethod4a() {}

  @Borrowed(value="return" /* is UNPARSEABLE */)
  public void annotatedMethod5a() {}

  @Borrowed(value="return, this" /* is UNPARSEABLE */)
  public void annotatedMethod6a() {}

  @Borrowed(value="this, return" /* is UNPARSEABLE */)
  public void annotatedMethod7a() {}

  @Borrowed(value=" f, a, b" /* is UNPARSEABLE */)
  public void annotatedMethod8a() {}



  protected class Constructor1 {
    @Borrowed
    protected Constructor1() {}
  }

  protected class Constructor2 {
    @Borrowed(/* is UNPARSEABLE */)
    protected Constructor2() {}
  }

  protected class Constructor3 {
    @Borrowed(""/* is UNPARSEABLE */)
    protected Constructor3() {}
  }

  protected class Constructor4 {
    @Borrowed("this" /* is CONSISTENT */)
    protected Constructor4() {}
  }

  protected class Constructor5 {
    @Borrowed("return" /* is UNPARSEABLE */)
    protected Constructor5() {}
  }

  protected class Constructor6 {
    @Borrowed("return, this" /* is UNPARSEABLE */)
    protected Constructor6() {}
  }

  protected class Constructor7 {
    @Borrowed("this, return" /* is UNPARSEABLE */)
    protected Constructor7() {}
  }

  protected class Constructor8 {
    @Borrowed(" f, a, b" /* is UNPARSEABLE */)
    protected Constructor8() {}
  }

  protected class Constructor3a {
    @Borrowed(value=""/* is UNPARSEABLE */)
    protected Constructor3a() {}
  }

  protected class Constructor4a {
    @Borrowed(value="this" /* is CONSISTENT */)
    protected Constructor4a() {}
  }

  protected class Constructor5a {
    @Borrowed(value="return" /* is UNPARSEABLE */)
    protected Constructor5a() {}
  }

  protected class Constructor6a {
    @Borrowed(value="return, this" /* is UNPARSEABLE */)
    protected Constructor6a() {}
  }

  protected class Constructor7a {
    @Borrowed(value="this, return" /* is UNPARSEABLE */)
    protected Constructor7a() {}
  }

  protected class Constructor8a {
    @Borrowed(value=" f, a, b" /* is UNPARSEABLE */)
    protected Constructor8a() {}
  }
}