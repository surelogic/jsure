package com.surelogic.common;

import edu.cmu.cs.fluid.java.FluidJavaRef;
import edu.cmu.cs.fluid.java.IFluidJavaRef;
import junit.framework.TestCase;

public final class TestJavaRef extends TestCase {

  public void testBuilder() {
    IJavaRef r = new JavaRef.Builder("java.lang/Object.A").build();
    assertEquals("java.lang", r.getPackageName());
    assertEquals("java/lang", r.getPackageNameSlash());
    assertEquals("java.lang", r.getPackageNameOrNull());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object$A", r.getTypeNameDollarSign());
    assertEquals("java.lang.Object.A", r.getTypeNameFullyQualified());
    assertEquals("java.lang/Object.A", r.getTypeNameFullyQualifiedSureLogic());
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());
    assertTrue(r.isFromSource());
    assertEquals(IJavaRef.TypeType.CLASS, r.getTypeType());
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
    assertEquals("java/lang", r.getPackageNameSlash());
    assertEquals("java.lang", r.getPackageNameOrNull());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object$A", r.getTypeNameDollarSign());
    assertEquals("java.lang.Object.A", r.getTypeNameFullyQualified());
    assertEquals("java.lang/Object.A", r.getTypeNameFullyQualifiedSureLogic());
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());
    assertTrue(r.isFromSource());
    assertEquals(IJavaRef.TypeType.CLASS, r.getTypeType());
    assertEquals("java/lang/Object.java", r.getClasspathRelativePathname());
    assertEquals("Object.java", r.getSimpleFileName());
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
    assertNull(r.getEclipseProjectNameOrNull());
    assertNull(r.getJavaId());
    assertNull(r.getEnclosingJavaId());
  }

  public void testFluidBuilder() {
    IFluidJavaRef r = new FluidJavaRef.Builder("java.lang/Object.A").build();
    assertEquals("java.lang", r.getPackageName());
    assertEquals("java/lang", r.getPackageNameSlash());
    assertEquals("java.lang", r.getPackageNameOrNull());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object$A", r.getTypeNameDollarSign());
    assertEquals("java.lang.Object.A", r.getTypeNameFullyQualified());
    assertEquals("java.lang/Object.A", r.getTypeNameFullyQualifiedSureLogic());
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());
    assertTrue(r.isFromSource());
    assertEquals(IJavaRef.TypeType.CLASS, r.getTypeType());
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
    assertEquals("java/lang", r.getPackageNameSlash());
    assertEquals("java.lang", r.getPackageNameOrNull());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object$A", r.getTypeNameDollarSign());
    assertEquals("java.lang.Object.A", r.getTypeNameFullyQualified());
    assertEquals("java.lang/Object.A", r.getTypeNameFullyQualifiedSureLogic());
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());
    assertTrue(r.isFromSource());
    assertEquals(IJavaRef.TypeType.CLASS, r.getTypeType());
    assertEquals("java/lang/Object.java", r.getClasspathRelativePathname());
    assertEquals("Object.java", r.getSimpleFileName());
    assertEquals(SLUtility.UNKNOWN_PROJECT, r.getEclipseProjectName());
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
    assertNull(r.getEclipseProjectNameOrNull());
    assertNull(r.getJavaId());
    assertNull(r.getEnclosingJavaId());
    assertNull(r.getWorkspaceRelativePathOrNull());
  }

  public void testDefaultPackage() {
    IJavaRef r = new JavaRef.Builder("/Object.A").build();
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, r.getPackageName());
    assertEquals("", r.getPackageNameSlash());
    assertNull(r.getPackageNameOrNull());

    r = new JavaRef.Builder("java.lang/Object.A").setPackageName("").build();
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, r.getPackageName());
    assertEquals("", r.getPackageNameSlash());
    assertNull(r.getPackageNameOrNull());
  }

  public void testChangingTypeName() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").setTypeName("Object.A").build();
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object.A", r.getTypeName());
    assertEquals("Object$A", r.getTypeNameDollarSign());
  }

  public void testAliasOfEncodedNamesJavaRef() {
    IJavaRef r = new JavaRef.Builder("java.lang/Object.A").build();
    String encodedNames = ((JavaRef) r).getEncodedNames();
    r = new JavaRef.Builder(r).build();
    assertSame(encodedNames, ((JavaRef) r).getEncodedNames());

    // mutate the encoded string a bit
    r = new JavaRef.Builder("java.lang/Object.A").setPackageName("java.lang").setTypeName("Object.A").build();
    encodedNames = ((JavaRef) r).getEncodedNames();
    r = new JavaRef.Builder(r).build();
    assertSame(encodedNames, ((JavaRef) r).getEncodedNames());
  }

  public void testAliasOfEncodedNamesFluidJavaRef() {
    IFluidJavaRef r = new FluidJavaRef.Builder("java.lang/Object.A").build();
    String encodedNames = ((JavaRef) r).getEncodedNames();
    r = new FluidJavaRef.Builder(r).build();
    assertSame(encodedNames, ((JavaRef) r).getEncodedNames());

    // mutate the encoded string a bit
    r = new FluidJavaRef.Builder("java.lang/Object.A").setPackageName("java.lang").setTypeName("Object.A").build();
    encodedNames = ((JavaRef) r).getEncodedNames();
    r = new FluidJavaRef.Builder(r).build();
    assertSame(encodedNames, ((JavaRef) r).getEncodedNames());
  }

  public void testWithin() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").build();
    assertEquals(IJavaRef.Within.JAVA_FILE, r.getWithin());

    r = new JavaRef.Builder("java.lang/Foo").setWithin(IJavaRef.Within.JAR_FILE).build();
    assertEquals(IJavaRef.Within.JAR_FILE, r.getWithin());
  }

  public void testTypeType() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").build();
    assertEquals(IJavaRef.TypeType.CLASS, r.getTypeType());

    r = new JavaRef.Builder("java.lang/Foo").setTypeType(IJavaRef.TypeType.ENUM).build();
    assertEquals(IJavaRef.TypeType.ENUM, r.getTypeType());
  }

  public void testCUName() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").setCUName("Bar").build();
    assertEquals("java/lang/Bar.java", r.getClasspathRelativePathname());
    assertEquals("Bar.java", r.getSimpleFileName());

    r = new JavaRef.Builder("java.lang/Foo").setCUName("Bar").setWithin(IJavaRef.Within.CLASS_FILE).build();
    // should ignore
    assertEquals("java/lang/Foo.class", r.getClasspathRelativePathname());
    assertEquals("Foo.class", r.getSimpleFileName());

    r = new JavaRef.Builder("java.lang/Foo").setCUName("Bar").setWithin(IJavaRef.Within.JAR_FILE).build();
    // should ignore
    assertEquals("java/lang/Foo.class", r.getClasspathRelativePathname());
    assertEquals("Foo.class", r.getSimpleFileName());

    // check encoder ignores setting to same name
    r = new JavaRef.Builder("java.lang/Foo").setCUName("Foo").build();
    assertEquals("java/lang/Foo.java", r.getClasspathRelativePathname());
    assertEquals("Foo.java", r.getSimpleFileName());
    assertEquals(":java.lang/Foo|", ((JavaRef) r).getEncodedNames());
  }

  public void testLineNumber() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder("java.lang/Foo").setLineNumber(500).build();
    assertEquals(500, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());
  }

  public void testOffset() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder("java.lang/Foo").setOffset(500).build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(500, r.getOffset());
    assertEquals(-1, r.getLength());
  }

  public void testLength() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(-1, r.getLength());

    r = new JavaRef.Builder("java.lang/Foo").setLength(500).build();
    assertEquals(-1, r.getLineNumber());
    assertEquals(-1, r.getOffset());
    assertEquals(500, r.getLength());
  }

  public void testWorkspaceRelativePath() {
    IFluidJavaRef r = new FluidJavaRef.Builder("java.lang/Object.A").build();
    assertNull(r.getWorkspaceRelativePathOrNull());

    r = new FluidJavaRef.Builder("java.lang/Object.A").setWorkspaceRelativePath("prj/src/java/lang/Object.java").build();
    assertEquals("prj/src/java/lang/Object.java", r.getWorkspaceRelativePathOrNull());
  }

  public void testJavaIds() {
    IJavaRef r = new JavaRef.Builder("java.lang/Foo").setJavaId("javaId").build();
    assertEquals("javaId", r.getJavaId());
    assertNull(r.getEnclosingJavaId());

    r = new JavaRef.Builder("java.lang/Foo").setEnclosingJavaId("javaId").build();
    assertNull(r.getJavaId());
    assertEquals("javaId", r.getEnclosingJavaId());
  }

}
