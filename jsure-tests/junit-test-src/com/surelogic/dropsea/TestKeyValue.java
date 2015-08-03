package com.surelogic.dropsea;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.surelogic.common.ref.Decl;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.IJavaRef.Within;
import com.surelogic.common.ref.JavaRef;

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

  public void testJavaRefValue() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    JavaRef.Builder b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAVA_FILE);
    b.setAbsolutePath("C:\\Program Files\\src\\java\\lang\\Object.java");
    IJavaRef ref = b.build();

    IKeyValue di = KeyValueUtility.getJavaRefInstance("KEY-JAVAREF", ref);
    assertEquals("KEY-JAVAREF", di.getKey());
    assertEquals(ref.encodeForPersistence(), di.getValueAsString());
    assertEquals(ref, di.getValueAsJavaRefOrNull());
    assertEquals(ref, di.getValueAsJavaRefOrThrow());
    assertEquals("Object.java", di.getValueAsJavaRefOrThrow().getSimpleFileName());
    assertEquals("Object", di.getValueAsJavaRefOrThrow().getSimpleFileNameWithNoExtension());
  }

  public void testDeclValue() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    IKeyValue di = KeyValueUtility.getDeclInstance("KEY-DECL", decl);
    assertEquals("KEY-DECL", di.getKey());
    assertEquals(Decl.encodeForPersistence(decl), di.getValueAsString());
    assertEquals(decl, di.getValueAsDeclOrNull());
    assertEquals(decl, di.getValueAsDeclOrThrow());
  }

  public void testIKeyValuePersistenceAndValueObject() {
    IKeyValue t1 = KeyValueUtility.getStringInstance("KEY", "this is a \n long test\t\t\tof the value");
    IKeyValue t2 = KeyValueUtility.getIntInstance("KEYINT", 56);
    IKeyValue t3 = KeyValueUtility.getLongInstance("LONGKEY", 56l);
    IKeyValue t4 = KeyValueUtility.getEnumInstance("ENUMKEY", Tenum.LITTLE);
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    IKeyValue t5 = KeyValueUtility.getDeclInstance("KEY-DECL", decl);
    JavaRef.Builder b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAVA_FILE);
    b.setAbsolutePath("C:\\Program Files\\src\\java\\lang\\Object.java");
    IJavaRef ref = b.build();
    IKeyValue t6 = KeyValueUtility.getJavaRefInstance("KEY-JAVAREF", ref);

    IKeyValue c1 = KeyValueUtility.parseEncodedForPersistence(t1.encodeForPersistence());
    IKeyValue c2 = KeyValueUtility.parseEncodedForPersistence(t2.encodeForPersistence());
    IKeyValue c3 = KeyValueUtility.parseEncodedForPersistence(t3.encodeForPersistence());
    IKeyValue c4 = KeyValueUtility.parseEncodedForPersistence(t4.encodeForPersistence());
    IKeyValue c5 = KeyValueUtility.parseEncodedForPersistence(t5.encodeForPersistence());
    IKeyValue c6 = KeyValueUtility.parseEncodedForPersistence(t6.encodeForPersistence());

    assertEquals(t1, c1);
    assertNotSame(t1, c1);
    assertEquals(t2, c2);
    assertNotSame(t2, c2);
    assertEquals(t3, c3);
    assertNotSame(t3, c3);
    assertEquals(t4, c4);
    assertNotSame(t4, c4);
    assertEquals(t5, c5);
    assertNotSame(t5, c5);
    assertEquals(t6, c6);
    assertNotSame(t6, c6);

    assertEquals(t1.encodeForPersistence(), c1.encodeForPersistence());
    assertEquals(t2.encodeForPersistence(), c2.encodeForPersistence());
    assertEquals(t3.encodeForPersistence(), c3.encodeForPersistence());
    assertEquals(t4.encodeForPersistence(), c4.encodeForPersistence());
    assertEquals(t5.encodeForPersistence(), c5.encodeForPersistence());
    assertEquals(t6.encodeForPersistence(), c6.encodeForPersistence());
  }

  public void testIKeyValueListPersistence() {
    final String s = "this is a \n long test\t\t\tof the value    ";
    IKeyValue t1 = KeyValueUtility.getStringInstance("KEY", s);
    IKeyValue t2 = KeyValueUtility.getIntInstance("KEYINT", 56);
    IKeyValue t3 = KeyValueUtility.getLongInstance("LONGKEY", 56l);
    IKeyValue t4 = KeyValueUtility.getLongInstance("KEY-LONG", Long.MAX_VALUE);
    IKeyValue t5 = KeyValueUtility.getEnumInstance("ENUMKEY", Tenum.LITTLE);
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    IKeyValue t6 = KeyValueUtility.getDeclInstance("KEY-DECL", decl);
    JavaRef.Builder b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAVA_FILE);
    b.setAbsolutePath("C:\\Program Files\\src\\java\\lang\\Object.java");
    IJavaRef ref = b.build();
    IKeyValue t7 = KeyValueUtility.getJavaRefInstance("KEY-JAVAREF", ref);

    List<IKeyValue> l1 = new ArrayList<>();
    l1.add(t1);
    l1.add(t2);
    l1.add(t3);
    l1.add(t4);
    l1.add(t5);
    l1.add(t6);
    l1.add(t7);

    String el1 = KeyValueUtility.encodeListForPersistence(l1);

    List<IKeyValue> l2 = KeyValueUtility.parseListEncodedForPersistence(el1);
    String el2 = KeyValueUtility.encodeListForPersistence(l2);

    assertEquals(l1, l2);
    assertEquals(el1, el2);

    assertEquals(s, l2.get(0).getValueAsString());
  }

  public void testIKeyValueEmptyListPersistence() {
    List<IKeyValue> l1 = new ArrayList<>();

    String el1 = KeyValueUtility.encodeListForPersistence(l1);

    List<IKeyValue> l2 = KeyValueUtility.parseListEncodedForPersistence(el1);
    String el2 = KeyValueUtility.encodeListForPersistence(l2);

    assertEquals(l1, l2);
    assertEquals(el1, el2);
  }
}
