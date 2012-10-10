package com.surelogic.common.ref;

import junit.framework.TestCase;

import com.surelogic.common.SLUtility;

import edu.cmu.cs.fluid.java.FluidJavaRef;
import edu.cmu.cs.fluid.java.IFluidJavaRef;

public final class TestJavaRef extends TestCase {

  public void testBuilder() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A");
    IJavaRef r = new JavaRef.Builder(decl).build();
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
    assertEquals("Object.java", r.getSimpleFileName());
    assertEquals("java/lang/Object.java", r.getClasspathRelativePathname());
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertNull(r.getEclipseProjectNameOrNull());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
    assertNull(r.getJavaId());
    assertNull(r.getEnclosingJavaId());

    // check copy
    r = new JavaRef.Builder(r).build();
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
    assertEquals("Object.java", r.getSimpleFileName());
    assertEquals("java/lang/Object.java", r.getClasspathRelativePathname());
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertNull(r.getEclipseProjectNameOrNull());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
    assertNull(r.getJavaId());
    assertNull(r.getEnclosingJavaId());
  }

  public void testFluidBuilder() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A");
    IFluidJavaRef r = new FluidJavaRef.Builder(decl).build();
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
    assertEquals("Object.java", r.getSimpleFileName());
    assertEquals("java/lang/Object.java", r.getClasspathRelativePathname());
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertNull(r.getEclipseProjectNameOrNull());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
    assertNull(r.getJavaId());
    assertNull(r.getEnclosingJavaId());
    assertNull(r.getWorkspaceRelativePathOrNull());

    // check copy
    r = new FluidJavaRef.Builder(r).build();
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
    assertEquals("Object.java", r.getSimpleFileName());
    assertEquals("java/lang/Object.java", r.getClasspathRelativePathname());
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertNull(r.getEclipseProjectNameOrNull());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
    assertNull(r.getJavaId());
    assertNull(r.getEnclosingJavaId());
    assertNull(r.getWorkspaceRelativePathOrNull());
  }

  public void testDefaultPackage() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("/Object.A");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, r.getPackageName());
    assertEquals("", DeclUtil.getPackageNameSlash(r.getDeclaration()));
    assertNull(DeclUtil.getPackageNameOrNull(r.getDeclaration()));
    assertEquals("", DeclUtil.getPackageNameOrEmpty(r.getDeclaration()));
  }

  public void testWithin() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());

    r = new JavaRef.Builder(decl).setWithin(IJavaRef.Within.JAR_FILE).build();
    assertEquals(IJavaRef.Within.JAR_FILE, r.getWithin());
  }

  public void testTypeKind() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(IDecl.Kind.CLASS, DeclUtil.getTypeKind(r.getDeclaration()));
  }

  public void testLineNumber() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
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
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder(decl).setOffset(500).build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(500, r.getOffset());
    assertEquals(-1, r.getLength());
  }

  public void testLength() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder(decl).setLength(500).build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(500, r.getLength());
  }

  public void testWorkspaceRelativePath() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Object.A");
    IFluidJavaRef r = new FluidJavaRef.Builder(decl).build();
    assertNull(r.getWorkspaceRelativePathOrNull());

    r = new FluidJavaRef.Builder(decl).setWorkspaceRelativePath("prj/src/java/lang/Object.java").build();
    assertEquals("prj/src/java/lang/Object.java", r.getWorkspaceRelativePathOrNull());
  }

  public void testJavaIds() {
    IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic("java.lang/Foo");
    IJavaRef r = new JavaRef.Builder(decl).setJavaId("javaId").build();
    assertEquals("javaId", r.getJavaId());
    assertNull(r.getEnclosingJavaId());

    r = new JavaRef.Builder(decl).setEnclosingJavaId("javaId").build();
    assertNull(r.getJavaId());
    assertEquals("javaId", r.getEnclosingJavaId());
  }
}
