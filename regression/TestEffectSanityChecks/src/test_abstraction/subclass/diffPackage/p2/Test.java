package test_abstraction.subclass.diffPackage.p2;

import test_abstraction.subclass.diffPackage.p1.S;

import com.surelogic.RegionEffects;

public class Test extends S {
  @SuppressWarnings("unused")
  @RegionEffects("writes Public" /* is CONSISTENT */)
  private void privateMethodReceiver_Public() {}

  @SuppressWarnings("unused")
  @RegionEffects("writes Protected" /* is UNASSOCIATED: Region is from superclass that is in a different package */)
  private void privateMethodReceiver_Protected() {}

  @SuppressWarnings("unused")
  @RegionEffects("writes Default" /* is UNASSOCIATED */)
  private void privateMethodReceiver_Default() {}

  @SuppressWarnings("unused")
  @RegionEffects("writes Private" /* is UNASSOCIATED */)
  private void privateMethodReceiver_Private() {}


  @RegionEffects("writes Public" /* is CONSISTENT */)
  void defaultMethodReceiver_Public() {}

  @RegionEffects("writes Protected" /* is UNASSOCIATED: Region is from superclass that is in a different package */)
  void defaultMethodReceiver_Protected() {}

  @RegionEffects("writes Default" /* is UNASSOCIATED */)
  void defaultMethodReceiver_Default() {}

  @RegionEffects("writes Private" /* is UNASSOCIATED */)
  void defaultMethodReceiver_Private() {}


  @RegionEffects("writes Public" /* is CONSISTENT */)
  protected void protectedMethodReceiver_Public() {}

  @RegionEffects("writes Protected" /* is UNASSOCIATED: Region is from superclass that is in a different package */)
  protected void protectedMethodReceiver_Protected() {}

  @RegionEffects("writes Default" /* is UNASSOCIATED */)
  protected void protectedMethodReceiver_Default() {}

  @RegionEffects("writes Private" /* is UNASSOCIATED */)
  protected void protectedMethodReceiver_Private() {}


  @RegionEffects("writes Public" /* is CONSISTENT */)
  public void publicMethodReceiver_Public() {}

  @RegionEffects("writes Protected" /* is UNASSOCIATED */)
  public void publicMethodReceiver_Protected() {}

  @RegionEffects("writes Default" /* is UNASSOCIATED */)
  public void publicMethodReceiver_Default() {}

  @RegionEffects("writes Private" /* is UNASSOCIATED */)
  public void publicMethodReceiver_Private() {}




  @SuppressWarnings("unused")
  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  private void privateMethodParam_Public(final Test t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Protected" /* is UNASSOCIATED: Region is from superclass that is in a different package */)
  private void privateMethodParam_Protected(final Test t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  private void privateMethodParam_Default(final Test t) {}

  @SuppressWarnings("unused")
  @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
  private void privateMethodParam_Private(final Test t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  void defaultMethodParam_Public(final Test t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED: Region is from superclass that is in a different package */)
  void defaultMethodParam_Protected(final Test t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  void defaultMethodParam_Default(final Test t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
  void defaultMethodParam_Private(final Test t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  protected void protectedMethodParam_Public(final Test t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED: Region is from superclass that is in a different package */)
  protected void protectedMethodParam_Protected(final Test t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  protected void protectedMethodParam_Default(final Test t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
  protected void protectedMethodParam_Private(final Test t) {}


  @RegionEffects("writes t:Public" /* is CONSISTENT */)
  public void publicMethodParam_Public(final Test t) {}

  @RegionEffects("writes t:Protected" /* is UNASSOCIATED */)
  public void publicMethodParam_Protected(final Test t) {}

  @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
  public void publicMethodParam_Default(final Test t) {}

  @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
  public void publicMethodParam_Private(final Test t) {}
}
