package com.surelogic.dropsea;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public final class TestDiffInfo extends TestCase {

  public void testNoCommaInKey() {
    try {
      DiffInfoUtility.getStringInstance("K,EY", "test");
      fail("Key name allowed to contain a comma");
    } catch (Exception ignore) {
      // test passed
    }
    try {
      DiffInfoUtility.getIntInstance("K,EY", 56);
      fail("Key name allowed to contain a comma");
    } catch (Exception ignore) {
      // test passed
    }
    try {
      DiffInfoUtility.getLongInstance("K,EY", 56l);
      fail("Key name allowed to contain a comma");
    } catch (Exception ignore) {
      // test passed
    }
  }

  public void testStringValue() {
    IDiffInfo di = DiffInfoUtility.getStringInstance("KEY", "test");

    assertEquals("KEY", di.getKey());
    assertEquals("test", di.getValueAsString());
    assertEquals(-1, di.getValueAsInt(-1));
    assertEquals(-1, di.getValueAsLong(-1));
  }

  public void testIntValue() {
    IDiffInfo di = DiffInfoUtility.getIntInstance("KEY-INT", 56);

    assertEquals("KEY-INT", di.getKey());
    assertEquals("56", di.getValueAsString());
    assertEquals(56, di.getValueAsInt(-1));
    assertEquals(56, di.getValueAsLong(-1));
  }

  public void testLongValue() {
    IDiffInfo di = DiffInfoUtility.getLongInstance("KEY-LONG", 56l);

    assertEquals("KEY-LONG", di.getKey());
    assertEquals("56", di.getValueAsString());
    assertEquals(56, di.getValueAsInt(-1));
    assertEquals(56, di.getValueAsLong(-1));
  }

  public void testBigLongValue() {
    IDiffInfo di = DiffInfoUtility.getLongInstance("KEY-LONG", Long.MAX_VALUE);

    assertEquals("KEY-LONG", di.getKey());
    assertEquals(Long.toString(Long.MAX_VALUE), di.getValueAsString());
    assertEquals(-1, di.getValueAsInt(-1));
    assertEquals(Long.MAX_VALUE, di.getValueAsLong(-1));
  }

  public void testIDiffInfoPersistenceAndValueObject() {
    IDiffInfo t1 = DiffInfoUtility.getStringInstance("KEY", "this is a \n long test\t\t\tof the value");
    IDiffInfo t2 = DiffInfoUtility.getIntInstance("KEYINT", 56);
    IDiffInfo t3 = DiffInfoUtility.getLongInstance("LONGKEY", 56l);

    IDiffInfo c1 = DiffInfoUtility.parseEncodedForPersistence(t1.encodeForPersistence());
    IDiffInfo c2 = DiffInfoUtility.parseEncodedForPersistence(t2.encodeForPersistence());
    IDiffInfo c3 = DiffInfoUtility.parseEncodedForPersistence(t3.encodeForPersistence());

    assertEquals(t1, c1);
    assertNotSame(t1, c1);
    assertEquals(t2, c2);
    assertNotSame(t2, c2);
    assertEquals(t3, c3);
    assertNotSame(t3, c3);

    assertEquals(t1.encodeForPersistence(), c1.encodeForPersistence());
    assertEquals(t2.encodeForPersistence(), c2.encodeForPersistence());
    assertEquals(t3.encodeForPersistence(), c3.encodeForPersistence());
  }

  public void testIDiffInfoListPersistence() {
    final String s = "this is a \n long test\t\t\tof the value    ";
    IDiffInfo t1 = DiffInfoUtility.getStringInstance("KEY", s);
    IDiffInfo t2 = DiffInfoUtility.getIntInstance("KEYINT", 56);
    IDiffInfo t3 = DiffInfoUtility.getLongInstance("LONGKEY", 56l);
    IDiffInfo t4 = DiffInfoUtility.getLongInstance("KEY-LONG", Long.MAX_VALUE);

    List<IDiffInfo> l1 = new ArrayList<IDiffInfo>();
    l1.add(t1);
    l1.add(t2);
    l1.add(t3);
    l1.add(t4);

    String el1 = DiffInfoUtility.encodeListForPersistence(l1);

    List<IDiffInfo> l2 = DiffInfoUtility.parseListEncodedForPersistence(el1);
    String el2 = DiffInfoUtility.encodeListForPersistence(l2);

    assertEquals(l1, l2);
    assertEquals(el1, el2);

    assertEquals(s, l2.get(0).getValueAsString());
  }

  public void testIDiffInfoEmptyListPersistence() {
    List<IDiffInfo> l1 = new ArrayList<IDiffInfo>();

    String el1 = DiffInfoUtility.encodeListForPersistence(l1);

    List<IDiffInfo> l2 = DiffInfoUtility.parseListEncodedForPersistence(el1);
    String el2 = DiffInfoUtility.encodeListForPersistence(l2);

    assertEquals(l1, l2);
    assertEquals(el1, el2);
  }
}
