package edu.cmu.cs.fluid.sea.test;

import java.util.*;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import junit.framework.TestCase;

/**
 * JUnit testcase for the {@link Drop} class.
 */
public class TestDrop extends TestCase {

  static class MyDrop extends Drop {
	  // Nothing to add
  }

  private MyDrop d1, d2, d3, d4, d5, d6, d7;

  public void testDropBasics() {
    // add d2 via the single drop addition method
    d1.addDependent(d2);
    // add d3 and d4 via an array of drops
    d1.addDependents(new Drop[]{d3, d4});
    // add d5 and d6 via a Collection of drops
    List<Drop> dropList = new ArrayList<Drop>();
    dropList.add(d5);
    dropList.add(d6);
    d1.addDependents(dropList);
    // check that d2-d6 have only one depondent and that it is d1
    assertTrue("d1 is not the only depondent of d2",
        d2.getDeponents().size() == 1 || d2.getDependents().contains(d1));
    assertTrue("d1 is not the only depondent of d3",
        d3.getDeponents().size() == 1 || d3.getDependents().contains(d1));
    assertTrue("d1 is not the only depondent of d4",
        d4.getDeponents().size() == 1 || d4.getDependents().contains(d1));
    assertTrue("d1 is not the only depondent of d5",
        d5.getDeponents().size() == 1 || d5.getDependents().contains(d1));
    assertTrue("d1 is not the only depondent of d6",
        d6.getDeponents().size() == 1 || d6.getDependents().contains(d1));
    // check that d1 has 5 dependents and that they are d2-d6
    Set<Drop> d1Dependents = d1.getDependents();
    assertTrue("d1 hase " + d1Dependents.size() + " instead of 5", d1Dependents
        .size() == 5);
    Set<Drop> dependentSet = new HashSet<Drop>();
    dependentSet.add(d2);
    dependentSet.add(d3);
    dependentSet.add(d4);
    dependentSet.add(d5);
    dependentSet.add(d6);
    assertTrue("d1 is missing dependents", dependentSet
        .containsAll(d1Dependents));
    // add d7 as a dependent of d6
    d6.addDependent(d7);
    // test that invalidation of d6 also invalidates d7
    d6.invalidate();
    assertTrue("d6 is still valid after invalidation", !d6.isValid());
    assertTrue("d7 is still valid after invalidation of deponent d6", !d7
        .isValid());
    assertTrue("d1-d5 are still valid after invalidation of d6", d1.isValid()
        && d2.isValid() && d3.isValid() && d4.isValid() && d5.isValid());
    d5.invalidate();
    assertTrue("d5 is still valid after invalidation", !d5.isValid());
    assertTrue("d1-d4 are still valid after invalidation of d5", d1.isValid()
        && d2.isValid() && d3.isValid() && d4.isValid());
    d1.invalidate();
    assertTrue("d1-d4 are still valid after invalidation of d1", !d1.isValid()
        && !d2.isValid() && !d3.isValid() && !d4.isValid());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testDropExceptions() {
    // check that ClassCastException is thrown by addDependents(Collection) if
    // non-Drop objects exist within the Collection
    List dropList = new ArrayList();
    dropList.add(d2);
    dropList.add(d3);
    dropList.add(new ArrayList());
    try {
      d1.addDependents(dropList);
      fail("adding non-Drop dependents appears to work when it should not");
    } catch (ClassCastException e) {
      // Ok, this is what we expected
    }
    d1.invalidate();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    d1 = new MyDrop();
    d2 = new MyDrop();
    d3 = new MyDrop();
    d4 = new MyDrop();
    d5 = new MyDrop();
    d6 = new MyDrop();
    d7 = new MyDrop();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Sea.getDefault().invalidateAll();
  }
}