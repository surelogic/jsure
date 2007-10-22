package test_constructorVisibility;

import com.surelogic.Reads;
import com.surelogic.Writes;

/**
 * Tests that the regions named in targets in constructor effects annotations
 * are at least as visible as the constructor.  Here we are as exhaustive as
 * we could be, simply because it is too much work to test all combinations due
 * to the fact that constructors can only be differentiated by their parameter
 * types.
 * 
 * <p>Mainly we just want to make sure that we can correctly retrieve the
 * visibility of the constructor. 
 */
public class Test {
  /**
   * Constructor.
   * read effect.
   * Public region, public method: Good
   */
  // GOOD
  @Reads("any(test_constructorVisibility.C):PublicRegion" /* is CONSISTENT */)
  public Test() {}

  /**
   * Constructor.
   * write effect.
   * default region, protected method: bad
   */
  // GOOD
  @Reads("any(test_constructorVisibility.C):DefaultRegion" /* is UNASSOCIATED */)
  public Test(int x) {}

  /**
   * Constructor.
   * write effect.
   * Public region, protected method: Good
   */
  // GOOD
  @Writes("c:PublicRegion" /* is CONSISTENT */)
  protected Test(C c) {}

  /**
   * Constructor.
   * read effect.
   * Private region, private method: Good
   */
  // GOOD
  @Reads("test_constructorVisibility.C:PrivateRegion" /* is CONSISTENT */)
  private void Test(int a, int b) {}
}
