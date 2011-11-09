package test_abstraction.unrelatedClasses.nested;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

public class Test {
  @Regions({
    @Region("public Public"),
    @Region("protected Protected"),
    @Region("Default"),
    @Region("private Private")
  })
  public class Inner {
  }

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  private void privateMethodParam_Public(final Inner t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Protected" /* is CONSISTENT */)
  private void privateMethodParam_Protected(final Inner t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Default" /* is CONSISTENT */)
  private void privateMethodParam_Default(final Inner t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Private" /* is CONSISTENT: Can refer to private regions of nested classes */)
  private void privateMethodParam_Private(final Inner t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  void defaultMethodParam_Public(final Inner t) {}

  @RegionEffects("writes t:Protected" /* is CONSISTENT */)
  void defaultMethodParam_Protected(final Inner t) {}

  @RegionEffects("writes t:Default" /* is CONSISTENT */)
  void defaultMethodParam_Default(final Inner t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Breach of abstraction */)
  void defaultMethodParam_Private(final Inner t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  protected void protectedMethodParam_Public(final Inner t) {}

  @RegionEffects("writes t:Protected" /* is CONSISTENT */)
  protected void protectedMethodParam_Protected(final Inner t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  protected void protectedMethodParam_Default(final Inner t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Breach of abstraction */)
  protected void protectedMethodParam_Private(final Inner t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  public void publicMethodParam_Public(final Inner t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED */)
  public void publicMethodParam_Protected(final Inner t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  public void publicMethodParam_Default(final Inner t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Breach of abstraction */)
  public void publicMethodParam_Private(final Inner t) {}
}
