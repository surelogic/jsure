package unique;

import com.surelogic.Unique;

public class TestBadArgs {
  // === UNIQUE FIELDS
  
  @Unique  // CONSISTENT
  protected Object uniqueField1;

  @Unique(/* is CONSISTENT */)
  protected Object uniqueField2;
  
  @Unique("" /* is CONSISTENT */)
  protected Object uniqueField3;
  
  @Unique("  this  " /* is UNPARSEABLE */)
  protected Object uniqueField4;
  
  @Unique("  return  " /* is UNPARSEABLE */)
  protected Object uniqueField5;
  
  @Unique(" this , return " /* is UNPARSEABLE */)
  protected Object uniqueField6;
  
  @Unique("  return  , this  " /* is UNPARSEABLE */)
  protected Object uniqueField7;
  
  @Unique("  a  " /* is UNPARSEABLE */)
  protected Object uniqueField8;
  
  @Unique(value="" /* is CONSISTENT */)
  protected Object uniqueField3a;
  
  @Unique(value="  this  " /* is UNPARSEABLE */)
  protected Object uniqueField4a;
  
  @Unique(value="  return  " /* is UNPARSEABLE */)
  protected Object uniqueField5a;
  
  @Unique(value=" this , return " /* is UNPARSEABLE */)
  protected Object uniqueField6a;
  
  @Unique(value="  return  , this  " /* is UNPARSEABLE */)
  protected Object uniqueField7a;
  
  @Unique(value="  a  " /* is UNPARSEABLE */)
  protected Object uniqueField8a;


  // === UNIQUE PARAMETERS
  
  public void method1(@Unique Object p) {} // is CONSISTENT

  public void method2(@Unique(/* is CONSISTENT */) Object p) {}

  public void method3(@Unique("" /* is CONSISTENT */) Object p) {}

  public void method4(@Unique(" this " /* is UNPARSEABLE */) Object p) {}

  public void method5(@Unique(" return  " /* is UNPARSEABLE */) Object p) {}

  public void method6(@Unique(" this, return " /* is UNPARSEABLE */) Object p) {}

  public void method7(@Unique(" return , this " /* is UNPARSEABLE */) Object p) {}

  public void method8(@Unique(" a, b, c" /* is UNPARSEABLE */) Object p) {}

  public void method3a(@Unique(value="" /* is CONSISTENT */) Object p) {}

  public void method4a(@Unique(value=" this " /* is UNPARSEABLE */) Object p) {}

  public void method5a(@Unique(value=" return  " /* is UNPARSEABLE */) Object p) {}

  public void method6a(@Unique(value=" this, return " /* is UNPARSEABLE */) Object p) {}

  public void method7a(@Unique(value=" return , this " /* is UNPARSEABLE */) Object p) {}

  public void method8a(@Unique(value=" a, b, c" /* is UNPARSEABLE */) Object p) {}

  

  // === UNIQUE ON METHOD 
  
  @Unique // bad, but no where to put "is UNPARSEABLE"
  public void annotatedMethod1() {}

  @Unique(/* is UNPARSEABLE */)
  public void annotatedMethod2() {}

  @Unique("" /* is UNPARSEABLE */)
  public void annotatedMethod3() {}

  @Unique(" this " /* is CONSISTENT */)
  public void annotatedMethod4() {}

  @Unique(" return  " /* is CONSISTENT */)
  public void annotatedMethod5() {}

  @Unique(" this, return " /* is CONSISTENT */)
  public void annotatedMethod6() {}

  @Unique(" return , this " /* is CONSISTENT */)
  public void annotatedMethod7() {}

  @Unique(" a, b, c" /* is UNPARSEABLE */)
  public void annotatedMethod8() {}

  @Unique(value="" /* is UNPARSEABLE */)
  public void annotatedMethod3a() {}

  @Unique(value=" this " /* is CONSISTENT */)
  public void annotatedMethod4a() {}

  @Unique(value=" return  " /* is CONSISTENT */)
  public void annotatedMethod5a() {}

  @Unique(value=" this, return " /* is CONSISTENT */)
  public void annotatedMethod6a() {}

  @Unique(value=" return , this " /* is CONSISTENT */)
  public void annotatedMethod7a() {}

  @Unique(value=" a, b, c" /* is UNPARSEABLE */)
  public void annotatedMethod8a() {}

  @Unique(value=" this, a, b, c" /* is UNPARSEABLE */)
  public void annotatedMethod9a() {}
}
