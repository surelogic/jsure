package edu.cmu.cs.fluid.sea.test;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropEvent;
import edu.cmu.cs.fluid.sea.DropObserver;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.Sea;

public class TestSea extends TestCase {

  final private Sea sea = Sea.getDefault();

  class ADrop extends Drop {
    // Nothing to add
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
    Set<Drop> r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    Set<Drop> r1 = Sea.filterDropsOfType(Drop.class, r);
    assertTrue("Drop count is " + r1.size() + " should be 6", r1.size() == 6);

    Set<MyDrop> r2 = Sea.filterDropsOfType(MyDrop.class, r);
    assertTrue("Drop count is " + r2.size() + " should be 4", r2.size() == 4);

    Set<MySubDrop> r3 = Sea.filterDropsOfType(MySubDrop.class, r);
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
    Set<Drop> r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    Set<? extends MyDrop> r2 = Sea.filterDropsOfTypeMutate(MyDrop.class, r);
    assertTrue("Drop count is " + r2.size() + " should be 4", r2.size() == 4);
    assertEquals(r2, r);

    r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    Set<? extends MySubDrop> r3 = Sea.filterDropsOfTypeMutate(MySubDrop.class, r);
    assertTrue("Drop count is " + r3.size() + " should be 2", r3.size() == 2);
    assertEquals(r3, r);
    r.add(d1); // mutation violates up-cast
    try {
      for (MySubDrop d : r3) {
        d.toString(); // make compiler happy
      }
      fail();
    } catch (ClassCastException e) {
      // Ignore, this is what is expected
    }

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
    r.add(d1); // mutation violates up-cast
    try {
      for (MySubDrop d : r3) {
        d.toString(); // make compiler happy
      }
      fail();
    } catch (ClassCastException e) {
      // Ignore, this is what is expected
    }
  }

  public void testFilter() {
    Set<Drop> r = sea.getDrops();
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);

    Set<Drop> r1 = Sea.filter(DropPredicateFactory.matchType(ADrop.class), r);
    assertTrue("Drop count is " + r1.size() + " should be 6", r1.size() == 6);
    r1 = Sea.filter(DropPredicateFactory.matchType(MyDrop.class), r);
    assertTrue("Drop count is " + r1.size() + " should be 4", r1.size() == 4);

    Sea.filterMutate(DropPredicateFactory.matchType(ADrop.class), r);
    assertTrue("Drop count is " + r.size() + " should be 6", r.size() == 6);
    Sea.filterMutate(DropPredicateFactory.matchType(MyDrop.class), r);
    assertTrue("Drop count is " + r.size() + " should be 4", r.size() == 4);
  }

  public void testHasMatchingDrops() {
    Set<MyDrop> r2 = Sea.filterDropsOfExactTypeMutate(MyDrop.class, Sea.getDefault().getDrops());
    assertTrue("Drop count is " + r2.size() + " should be 2", r2.size() == 2);
    assertFalse(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(Drop.class), r2));
    assertFalse(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(ADrop.class), r2));
    assertTrue(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(MyDrop.class), r2));
    assertFalse(Sea.hasMatchingDrops(DropPredicateFactory.matchExactType(MySubDrop.class), r2));
  }

  public void testAddMatchingDrops() {
    Set<Drop> r = sea.getDrops();
    Set<Drop> r1 = new HashSet<Drop>();
    Sea.addMatchingDropsFrom(r, DropPredicateFactory.matchExactType(ADrop.class), r1);
    Sea.filterDropsOfExactTypeMutate(ADrop.class, r);
    assertTrue(r1.equals(r));
  }

  public void testGetDropsOfType() {
    // test getting drops of type (i.e., including subtypes)
    Set<Drop> dropSet = sea.getDropsOfType(Drop.class); // should get all
    // drops
    assertTrue("Drop count is " + dropSet.size() + " should be 6", dropSet.size() == 6);
    Set<Drop> n = new HashSet<Drop>();
    n.add(d1);
    n.add(d2);
    n.add(md1);
    n.add(md2);
    n.add(msd1);
    n.add(msd2);
    assertTrue("Drop instances are wrong", dropSet.containsAll(n));
    Set<MyDrop> myDropSet = sea.getDropsOfType(MyDrop.class);
    assertTrue("MyDrop count is " + myDropSet.size() + " should be 4", myDropSet.size() == 4);
    n = new HashSet<Drop>();
    n.add(md1);
    n.add(md2);
    n.add(msd1);
    n.add(msd2);
    assertTrue("MyDrop instances are wrong", myDropSet.containsAll(n));
    Set<MySubDrop> mySubDropSet = sea.getDropsOfType(MySubDrop.class);
    assertTrue("MySubDrop count is " + mySubDropSet.size() + " should be 2", mySubDropSet.size() == 2);
    n = new HashSet<Drop>();
    n.add(msd1);
    n.add(msd2);
    assertTrue("MySubDrop instances are wrong", mySubDropSet.containsAll(n));
  }

  public void testGetDropsOfExactType() {
    Sea sea = Sea.getDefault();
    // test getting drops of a exact type
    Set<ADrop> dropSet = sea.getDropsOfExactType(ADrop.class);
    assertTrue("Drop count is " + dropSet.size() + " should be 2", dropSet.size() == 2);
    Set<Drop> n = new HashSet<Drop>();
    n.add(d1);
    n.add(d2);
    assertTrue("Drop instances are wrong", dropSet.containsAll(n));
    Set<MyDrop> myDropSet = sea.getDropsOfExactType(MyDrop.class);
    assertTrue("MyDrop count is " + myDropSet.size() + " should be 2", myDropSet.size() == 2);
    n = new HashSet<Drop>();
    n.add(md1);
    n.add(md2);
    assertTrue("MyDrop instances are wrong", myDropSet.containsAll(n));
    Set<MySubDrop> mySubDropSet = sea.getDropsOfType(MySubDrop.class);
    assertTrue("MySubDrop count is " + mySubDropSet.size() + " should be 2", mySubDropSet.size() == 2);
    n = new HashSet<Drop>();
    n.add(msd1);
    n.add(msd2);
    assertTrue("MySubDrop instances are wrong", mySubDropSet.containsAll(n));
  }

  public void testListeners() {
    Sea sea = Sea.getDefault();
    DropObserver callback = new DropObserver() {
      @Override
      public void dropChanged(Drop drop, DropEvent event) {
        assertTrue("kind is not Created it is " + event, event == DropEvent.Created);
      }
    };
    sea.register(MySubDrop.class, callback);
    Drop dIgnore = new ADrop(); // shouldn't be noticed
    dIgnore.invalidate();
    MySubDrop dHearMe = new MySubDrop();
    sea.unregister(MySubDrop.class, callback);
    callback = new DropObserver() {
      @Override
      public void dropChanged(Drop drop, DropEvent event) {
        assertTrue("kind is not Invalidated it is " + event, event == DropEvent.Invalidated);
      }
    };
    sea.register(MySubDrop.class, callback);
    dHearMe.invalidate();
    sea.unregister(MySubDrop.class, callback);
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