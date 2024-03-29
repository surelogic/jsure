package com.surelogic.dropsea;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.surelogic.NonNull;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.DropPredicateFactory;
import com.surelogic.dropsea.ir.Sea;

import edu.cmu.cs.fluid.ir.PlainIRNode;

public class TestSea extends TestCase {

  final private Sea sea = Sea.getDefault();

  class ADrop extends Drop {
    protected ADrop() {
      super(new PlainIRNode());
    }

    @NonNull
    @Override
    public DropType getDropType() {
      return DropType.OTHER;
    }
  }

  class MyDrop extends ADrop {

    public String foo = "myDrop";
  }

  class MySubDrop extends MyDrop {

    public int bar = 5;
  }

  private ADrop d1, d2;

  private MyDrop md1, md2;

  private MySubDrop msd1, msd2;

  public void testFiterDrops() {
    List<Drop> r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    List<Drop> r1 = Sea.filterDropsOfType(Drop.class, r);
    assertTrue("Drop count is " + r1.size() + " should be 6", r1.size() == 6);

    List<MyDrop> r2 = Sea.filterDropsOfType(MyDrop.class, r);
    assertTrue("Drop count is " + r2.size() + " should be 4", r2.size() == 4);

    List<MySubDrop> r3 = Sea.filterDropsOfType(MySubDrop.class, r);
    assertTrue("Drop count is " + r3.size() + " should be 2", r3.size() == 2);

    r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    r1 = Sea.filterDropsOfExactType(Drop.class, r);
    assertTrue("Drop count is " + r1.size() + " should be 0", r1.size() == 0);

    r2 = Sea.filterDropsOfExactType(MyDrop.class, r);
    assertTrue("Drop count is " + r2.size() + " should be 2", r2.size() == 2);

    r3 = Sea.filterDropsOfExactType(MySubDrop.class, r);
    assertTrue("Drop count is " + r3.size() + " should be 2", r3.size() == 2);
  }

  public void testFilterDropsMutable() {
    List<Drop> r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    List<Drop> r2 = Sea.filterDropsOfTypeMutate(MyDrop.class, r);
    assertTrue("Drop count is " + r2.size() + " should be 4", r2.size() == 4);
    assertEquals(r2, r);

    r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    List<Drop> r3 = Sea.filterDropsOfTypeMutate(MySubDrop.class, r);
    assertTrue("Drop count is " + r3.size() + " should be 2", r3.size() == 2);
    assertEquals(r3, r);

    r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    r2 = Sea.filterDropsOfExactTypeMutate(MyDrop.class, r);
    assertTrue("Drop count is " + r2.size() + " should be 2", r2.size() == 2);
    assertEquals(r2, r);

    r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    r3 = Sea.filterDropsOfExactTypeMutate(MySubDrop.class, r);
    assertTrue("Drop count is " + r3.size() + " should be 2", r3.size() == 2);
    assertEquals(r3, r);
  }

  public void testFilter() {
    List<Drop> r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    List<Drop> r1 = Sea.filterDropsMatching(DropPredicateFactory.matchType(ADrop.class), r);
    assertTrue("Drop count is " + r1.size() + " should be 6", r1.size() == 6);
    r1 = Sea.filterDropsMatching(DropPredicateFactory.matchType(MyDrop.class), r);
    assertTrue("Drop count is " + r1.size() + " should be 4", r1.size() == 4);

    List<Drop> rr = Sea.filterDropsMatchingMutate(DropPredicateFactory.matchType(ADrop.class), r);
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);
    assertSame(rr, r);
    rr = Sea.filterDropsMatchingMutate(DropPredicateFactory.matchType(MyDrop.class), r);
    assertTrue("Drop count is " + r.size() + " should be 4", r.size() == 4);
    assertSame(rr, r);
  }

  public void testHasMatchingDrops() {
    List<Drop> r2 = Sea.filterDropsOfExactTypeMutate(MyDrop.class, Sea.getDefault().getDrops());
    assertTrue("Drop count is " + r2.size() + " should be 2", r2.size() == 2);
    assertFalse(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(Drop.class), r2));
    assertFalse(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(ADrop.class), r2));
    assertTrue(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(MyDrop.class), r2));
    assertFalse(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(MySubDrop.class), r2));
  }

  public void testGetDropsOfType() {
    // test getting drops of type (i.e., including subtypes)
    List<Drop> dropSet = sea.getDropsOfType(Drop.class); // should get all
    // drops
    assertTrue("Drop count is " + dropSet.size() + " should be 6", dropSet.size() == 6);
    Set<Drop> n = new HashSet<>();
    n.add(d1);
    n.add(d2);
    n.add(md1);
    n.add(md2);
    n.add(msd1);
    n.add(msd2);
    assertTrue("Drop instances are wrong", dropSet.containsAll(n));
    List<MyDrop> myDropSet = sea.getDropsOfType(MyDrop.class);
    assertTrue("MyDrop count is " + myDropSet.size() + " should be 4", myDropSet.size() == 4);
    n = new HashSet<>();
    n.add(md1);
    n.add(md2);
    n.add(msd1);
    n.add(msd2);
    assertTrue("MyDrop instances are wrong", myDropSet.containsAll(n));
    List<MySubDrop> mySubDropSet = sea.getDropsOfType(MySubDrop.class);
    assertTrue("MySubDrop count is " + mySubDropSet.size() + " should be 2", mySubDropSet.size() == 2);
    n = new HashSet<>();
    n.add(msd1);
    n.add(msd2);
    assertTrue("MySubDrop instances are wrong", mySubDropSet.containsAll(n));
  }

  public void testGetDropsOfExactType() {
    Sea sea = Sea.getDefault();
    // test getting drops of a exact type
    List<ADrop> dropSet = sea.getDropsOfExactType(ADrop.class);
    assertTrue("Drop count is " + dropSet.size() + " should be 2", dropSet.size() == 2);
    Set<Drop> n = new HashSet<>();
    n.add(d1);
    n.add(d2);
    assertTrue("Drop instances are wrong", dropSet.containsAll(n));
    List<MyDrop> myDropSet = sea.getDropsOfExactType(MyDrop.class);
    assertTrue("MyDrop count is " + myDropSet.size() + " should be 2", myDropSet.size() == 2);
    n = new HashSet<>();
    n.add(md1);
    n.add(md2);
    assertTrue("MyDrop instances are wrong", myDropSet.containsAll(n));
    List<MySubDrop> mySubDropSet = sea.getDropsOfType(MySubDrop.class);
    assertTrue("MySubDrop count is " + mySubDropSet.size() + " should be 2", mySubDropSet.size() == 2);
    n = new HashSet<>();
    n.add(msd1);
    n.add(msd2);
    assertTrue("MySubDrop instances are wrong", mySubDropSet.containsAll(n));
  }

  public void testMarkNewDrops() {
    d1.addDependent(d2);
    d1.addDependent(msd1);
    d1.invalidate();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    d1 = new ADrop();
    d2 = new ADrop();
    md1 = new MyDrop();
    md2 = new MyDrop();
    msd1 = new MySubDrop();
    msd2 = new MySubDrop();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Sea.getDefault().invalidateAll();
    d1 = d2 = md1 = md2 = msd1 = msd2 = null;
  }
}