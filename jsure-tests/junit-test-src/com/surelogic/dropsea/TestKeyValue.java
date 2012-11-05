package com.surelogic.dropsea;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public final class TestKeyValue extends TestCase {

  public void testNoCommaInKey() {
    try {
      KeyValueUtility.getStringInstance("K,EY", "test");
      fail("Key name allowed to contain a comma");
    } catch (Exception ignore) {
      // test passed
    }
    try {
      KeyValueUtility.getIntInstance("K,EY", 56);
      fail("Key name allowed to contain a comma");
    } catch (Exception ignore) {
      // test passed
    }
    try {
      KeyValueUtility.getLongInstance("K,EY", 56l);
      fail("Key name allowed to contain a comma");
    } catch (Exception ignore) {
      // test passed
    }
  }

  public void testStringValue() {
    IKeyValue di = KeyValueUtility.getStringInstance("KEY", "test");

    assertEquals("KEY", di.getKey());
    assertEquals("test", di.getValueAsString());
    assertEquals(-1, di.getValueAsInt(-1));
    assertEquals(-1, di.getValueAsLong(-1));
  }

  public void testIntValue() {
    IKeyValue di = KeyValueUtility.getIntInstance("KEY-INT", 56);

    assertEquals("KEY-INT", di.getKey());
    assertEquals("56", di.getValueAsString());
    assertEquals(56, di.getValueAsInt(-1));
    assertEquals(56, di.getValueAsLong(-1));
  }

  public void testLongValue() {
    IKeyValue di = KeyValueUtility.getLongInstance("KEY-LONG", 56l);

    assertEquals("KEY-LONG", di.getKey());
    assertEquals("56", di.getValueAsString());
    assertEquals(56, di.getValueAsInt(-1));
    assertEquals(56, di.getValueAsLong(-1));
  }

  public void testBigLongValue() {
    IKeyValue di = KeyValueUtility.getLongInstance("KEY-LONG", Long.MAX_VALUE);

    assertEquals("KEY-LONG", di.getKey());
    assertEquals(Long.toString(Long.MAX_VALUE), di.getValueAsString());
    assertEquals(-1, di.getValueAsInt(-1));
    assertEquals(Long.MAX_VALUE, di.getValueAsLong(-1));
  }

  enum Tenum {
    BIG, LITTLE, OTHER
  }

  public void testEnumValue() {
    IKeyValue di = KeyValueUtility.getEnumInstance("KEY-ENUM", Tenum.BIG);
    assertEquals("KEY-ENUM", di.getKey());
    assertEquals(Tenum.BIG.name(), di.getValueAsString());
    assertEquals(Tenum.BIG, di.getValueAsEnum(Tenum.OTHER, Tenum.class));
    assertEquals(-1, di.getValueAsInt(-1));
    assertEquals(-1, di.getValueAsLong(-1));

    IKeyValue bad = KeyValueUtility.getStringInstance("BAS", "BAD");
    assertEquals(Tenum.OTHER, bad.getValueAsEnum(Tenum.OTHER, Tenum.class));
  }

  public void testIKeyValuePersistenceAndValueObject() {
    IKeyValue t1 = KeyValueUtility.getStringInstance("KEY", "this is a \n long test\t\t\tof the value");
    IKeyValue t2 = KeyValueUtility.getIntInstance("KEYINT", 56);
    IKeyValue t3 = KeyValueUtility.getLongInstance("LONGKEY", 56l);
    IKeyValue t4 = KeyValueUtility.getEnumInstance("ENUMKEY", Tenum.LITTLE);

    IKeyValue c1 = KeyValueUtility.parseEncodedForPersistence(t1.encodeForPersistence());
    IKeyValue c2 = KeyValueUtility.parseEncodedForPersistence(t2.encodeForPersistence());
    IKeyValue c3 = KeyValueUtility.parseEncodedForPersistence(t3.encodeForPersistence());
    IKeyValue c4 = KeyValueUtility.parseEncodedForPersistence(t4.encodeForPersistence());

    assertEquals(t1, c1);
    assertNotSame(t1, c1);
    assertEquals(t2, c2);
    assertNotSame(t2, c2);
    assertEquals(t3, c3);
    assertNotSame(t3, c3);
    assertEquals(t4, c4);
    assertNotSame(t4, c4);

    assertEquals(t1.encodeForPersistence(), c1.encodeForPersistence());
    assertEquals(t2.encodeForPersistence(), c2.encodeForPersistence());
    assertEquals(t3.encodeForPersistence(), c3.encodeForPersistence());
    assertEquals(t4.encodeForPersistence(), c4.encodeForPersistence());
  }

  public void testIKeyValueListPersistence() {
    final String s = "this is a \n long test\t\t\tof the value    ";
    IKeyValue t1 = KeyValueUtility.getStringInstance("KEY", s);
    IKeyValue t2 = KeyValueUtility.getIntInstance("KEYINT", 56);
    IKeyValue t3 = KeyValueUtility.getLongInstance("LONGKEY", 56l);
    IKeyValue t4 = KeyValueUtility.getLongInstance("KEY-LONG", Long.MAX_VALUE);
    IKeyValue t5 = KeyValueUtility.getEnumInstance("ENUMKEY", Tenum.LITTLE);

    List<IKeyValue> l1 = new ArrayList<IKeyValue>();
    l1.add(t1);
    l1.add(t2);
    l1.add(t3);
    l1.add(t4);
    l1.add(t5);

    String el1 = KeyValueUtility.encodeListForPersistence(l1);

    List<IKeyValue> l2 = KeyValueUtility.parseListEncodedForPersistence(el1);
    String el2 = KeyValueUtility.encodeListForPersistence(l2);

    assertEquals(l1, l2);
    assertEquals(el1, el2);

    assertEquals(s, l2.get(0).getValueAsString());
  }

  public void testIKeyValueEmptyListPersistence() {
    List<IKeyValue> l1 = new ArrayList<IKeyValue>();

    String el1 = KeyValueUtility.encodeListForPersistence(l1);

    List<IKeyValue> l2 = KeyValueUtility.parseListEncodedForPersistence(el1);
    String el2 = KeyValueUtility.encodeListForPersistence(l2);

    assertEquals(l1, l2);
    assertEquals(el1, el2);
  }
}