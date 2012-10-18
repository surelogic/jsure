package com.surelogic.common.ref;

import junit.framework.TestCase;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IJavaRef.Within;

public final class TestJavaRef extends TestCase {

  public void testBuilder() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals("java.lang", r.getPackageName());
    assertEquals("java/lang", DeclUtil.getPackageNameSlash(r.getDeclaration()));
    assertEquals("java.lang", DeclUtil.getPackageNameOrNull(r.getDeclaration()));
    assertEquals("java.lang", DeclUtil.getPackageNameOrEmpty(r.getDeclaration()));
    assertEquals("Object.A", r.getTypeNameOrNull());
    assertEquals("Object$A", DeclUtil.getTypeNameDollarSignOrNull(r.getDeclaration()));
    assertEquals("java.lang.Object.A", r.getTypeNameFullyQualified());
    assertEquals("java.lang/Object.A", DeclUtil.getTypeNameFullyQualifiedSureLogic(r.getDeclaration()));
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());
    assertTrue(r.isFromSource());
    assertEquals(IDecl.Kind.CLASS, DeclUtil.getTypeKind(r.getDeclaration()));
    assertEquals("Object.java", DeclUtil.guessSimpleFileName(r.getDeclaration(), r.getWithin()));
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertNull(r.getEclipseProjectNameOrNull());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    // check copy
    r = new JavaRef.Builder(r).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals("java.lang", r.getPackageName());
    assertEquals("java/lang", DeclUtil.getPackageNameSlash(r.getDeclaration()));
    assertEquals("java.lang", DeclUtil.getPackageNameOrNull(r.getDeclaration()));
    assertEquals("java.lang", DeclUtil.getPackageNameOrEmpty(r.getDeclaration()));
    assertEquals("Object.A", r.getTypeNameOrNull());
    assertEquals("Object$A", DeclUtil.getTypeNameDollarSignOrNull(r.getDeclaration()));
    assertEquals("java.lang.Object.A", r.getTypeNameFullyQualified());
    assertEquals("java.lang/Object.A", DeclUtil.getTypeNameFullyQualifiedSureLogic(r.getDeclaration()));
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());
    assertTrue(r.isFromSource());
    assertEquals(IDecl.Kind.CLASS, DeclUtil.getTypeKind(r.getDeclaration()));
    assertEquals("Object.java", DeclUtil.guessSimpleFileName(r.getDeclaration(), r.getWithin()));
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertNull(r.getEclipseProjectNameOrNull());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
  }

  public void testDefaultPackage() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("/Object.A");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, r.getPackageName());
    assertEquals("", DeclUtil.getPackageNameSlash(r.getDeclaration()));
    assertNull(DeclUtil.getPackageNameOrNull(r.getDeclaration()));
    assertEquals("", DeclUtil.getPackageNameOrEmpty(r.getDeclaration()));
  }

  public void testWithin() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());

    r = new JavaRef.Builder(decl).setWithin(IJavaRef.Within.JAR_FILE).build();
    assertEquals(IJavaRef.Within.JAR_FILE, r.getWithin());
  }

  public void testTypeKind() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(IDecl.Kind.CLASS, DeclUtil.getTypeKind(r.getDeclaration()));
  }

  public void testLineNumber() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder(decl).setLineNumber(500).build();
    assertEquals(500, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
  }

  public void testOffset() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder(decl).setOffset(500).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(-1, r.getLineNumber());
    assertEquals(500, r.getOffset());
    assertEquals(-1, r.getLength());
  }

  public void testLength() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder(decl).setLength(500).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(500, r.getLength());
  }

  public void testWorkspaceRelativePath() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(r.encodeForPersistence(), JavaRef.parseEncodedForPersistence(r.encodeForPersistence()).encodeForPersistence());
    assertNull(r.getAbsolutePathOrNull());

    r = new JavaRef.Builder(decl).setAbsolutePath("prj/src/java/lang/Object.java").build();
    assertEquals("prj/src/java/lang/Object.java", r.getAbsolutePathOrNull());
  }

  public void testSimpleFileName() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    JavaRef.Builder b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAVA_FILE);
    b.setAbsolutePath("C:\\Program Files\\src\\java\\lang\\Object.java");
    IJavaRef ref = b.build();
    assertEquals("Object.java", ref.getSimpleFileName());
    assertEquals("Object", ref.getSimpleFileNameWithNoExtension());

    decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAVA_FILE);
    b.setAbsolutePath("C:\\Program Files\\src\\java\\lang\\Goo.java");
    ref = b.build();
    assertEquals("Goo.java", ref.getSimpleFileName());
    assertEquals("Goo", ref.getSimpleFileNameWithNoExtension());

    decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAVA_FILE);
    ref = b.build();
    assertEquals("Object.java", ref.getSimpleFileName());
    assertEquals("Object", ref.getSimpleFileNameWithNoExtension());

    decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    b = new JavaRef.Builder(decl);
    b.setWithin(Within.CLASS_FILE);
    ref = b.build();
    assertEquals("Object$A$B.class", ref.getSimpleFileName());
    assertEquals("Object$A$B", ref.getSimpleFileNameWithNoExtension());

    decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A.B");
    b = new JavaRef.Builder(decl);
    b.setWithin(Within.JAR_FILE);
    ref = b.build();
    assertEquals("Object$A$B.class", ref.getSimpleFileName());
    assertEquals("Object$A$B", ref.getSimpleFileNameWithNoExtension());
  }
}
