package test_abstraction.unrelatedClasses.diffPackage.p1;

import test_abstraction.unrelatedClasses.diffPackage.p2.Other;

import com.surelogic.RegionEffects;

public class Test {
  @SuppressWarnings("unused")
  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  private void privateMethodParam_Public(final Other t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Protected" /* is UNASSOCIATED: Cannot refer to protected regions from different packages */)
  private void privateMethodParam_Protected(final Other t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Default" /* is UNASSOCIATED: Cannot refer to default regions from different packages */)
  private void privateMethodParam_Default(final Other t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Cannot refer to private regions of other classes */)
  private void privateMethodParam_Private(final Other t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  void defaultMethodParam_Public(final Other t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED: Cannot refer to protected regions from different packages */)
  void defaultMethodParam_Protected(final Other t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED: Cannot refer to default regions from different packages */)
  void defaultMethodParam_Default(final Other t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Cannot refer to private regions of other classes */)
  void defaultMethodParam_Private(final Other t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  protected void protectedMethodParam_Public(final Other t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED: Cannot refer to protected regions from different packages */)
  protected void protectedMethodParam_Protected(final Other t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  protected void protectedMethodParam_Default(final Other t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Cannot refer to private regions of other classes */)
  protected void protectedMethodParam_Private(final Other t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  public void publicMethodParam_Public(final Other t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED */)
  public void publicMethodParam_Protected(final Other t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  public void publicMethodParam_Default(final Other t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED: Cannot refer to private regions of other classes */)
  public void publicMethodParam_Private(final Other t) {}
}
