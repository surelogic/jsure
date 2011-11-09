package test_abstraction.unrelatedClasses.nested;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public Public"),
  @Region("protected Protected"),
  @Region("Default"),
  @Region("private Private")
})
public class Other {
  public class InnerTest {
    @SuppressWarnings("unused")
    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Public" /* is CONSISTENT */)
    private void privateMethodReceiver_Public() {}

    @SuppressWarnings("unused")
    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Protected" /* is CONSISTENT */)
    private void privateMethodReceiver_Protected() {}

    @SuppressWarnings("unused")
    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Default" /* is CONSISTENT */)
    private void privateMethodReceiver_Default() {}

    @SuppressWarnings("unused")
    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Private" /* is CONSISTENT */)
    private void privateMethodReceiver_Private() {}


    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Public" /* is CONSISTENT */)
    void defaultMethodReceiver_Public() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Protected" /* is CONSISTENT */)
    void defaultMethodReceiver_Protected() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Default" /* is CONSISTENT */)
    void defaultMethodReceiver_Default() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Private" /* is UNASSOCIATED */)
    void defaultMethodReceiver_Private() {}


    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Public" /* is CONSISTENT */)
    protected void protectedMethodReceiver_Public() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Protected" /* is CONSISTENT */)
    protected void protectedMethodReceiver_Protected() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Default" /* is UNASSOCIATED */)
    protected void protectedMethodReceiver_Default() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Private" /* is UNASSOCIATED */)
    protected void protectedMethodReceiver_Private() {}


    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Public" /* is CONSISTENT */)
    public void publicMethodReceiver_Public() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Protected" /* is UNASSOCIATED */)
    public void publicMethodReceiver_Protected() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Default" /* is UNASSOCIATED */)
    public void publicMethodReceiver_Default() {}

    @RegionEffects("writes test_abstraction.unrelatedClasses.nested.Other.this:Private" /* is UNASSOCIATED */)
    public void publicMethodReceiver_Private() {}




    @SuppressWarnings("unused")
    @RegionEffects("writes t:Public" /* is CONSISTENT */)
    private void privateMethodParam_Public(final Other t) {}

    @SuppressWarnings("unused")
    @RegionEffects("writes t:Protected" /* is CONSISTENT */)
    private void privateMethodParam_Protected(final Other t) {}

    @SuppressWarnings("unused")
    @RegionEffects("writes t:Default" /* is CONSISTENT */)
    private void privateMethodParam_Default(final Other t) {}

    @SuppressWarnings("unused")
    @RegionEffects("writes t:Private" /* is CONSISTENT */)
    private void privateMethodParam_Private(final Other t) {}


    @RegionEffects("writes t:Public" /* is CONSISTENT */)
    void defaultMethodParam_Public(final Other t) {}

    @RegionEffects("writes t:Protected" /* is CONSISTENT */)
    void defaultMethodParam_Protected(final Other t) {}

    @RegionEffects("writes t:Default" /* is CONSISTENT */)
    void defaultMethodParam_Default(final Other t) {}

    @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
    void defaultMethodParam_Private(final Other t) {}


    @RegionEffects("writes t:Public" /* is CONSISTENT */)
    protected void protectedMethodParam_Public(final Other t) {}

    @RegionEffects("writes t:Protected" /* is CONSISTENT */)
    protected void protectedMethodParam_Protected(final Other t) {}

    @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
    protected void protectedMethodParam_Default(final Other t) {}

    @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
    protected void protectedMethodParam_Private(final Other t) {}


    @RegionEffects("writes t:Public" /* is CONSISTENT */)
    public void publicMethodParam_Public(final Other t) {}

    @RegionEffects("writes t:Protected" /* is UNASSOCIATED */)
    public void publicMethodParam_Protected(final Other t) {}

    @RegionEffects("writes t:Default" /* is UNASSOCIATED */)
    public void publicMethodParam_Default(final Other t) {}

    @RegionEffects("writes t:Private" /* is UNASSOCIATED */)
    public void publicMethodParam_Private(final Other t) {}
  }
}
