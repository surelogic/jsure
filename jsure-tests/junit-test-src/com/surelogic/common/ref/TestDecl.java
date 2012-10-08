package com.surelogic.common.ref;

import junit.framework.TestCase;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IDecl.Visibility;

public class TestDecl extends TestCase {

  public void testClassBuilder() {
    IDecl p = new Decl.ClassBuilder("Foo").build();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    assertEquals(0, p.getParameters().length);
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    Decl.ClassBuilder inner = new Decl.ClassBuilder("Inner");
    Decl.ClassBuilder outer = new Decl.ClassBuilder("Outer");
    Decl.PackageBuilder pkg = new Decl.PackageBuilder("org.apache");
    outer.setParent(pkg);
    inner.setParent(outer);
    inner.setVisibility(Visibility.PRIVATE);
    inner.setIsStatic(true);
    outer.setIsFinal(true);
    p = inner.build();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Inner", p.getName());
    assertSame(Visibility.PRIVATE, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertTrue(p.isStatic());
    assertFalse(p.isFinal());
    p = p.getParent();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Outer", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertTrue(p.isFinal());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("apache", p.getName());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("org", p.getName());
    assertNull(p.getParent());

    // TODO
    // p = new Decl.ClassBuilder("Foo").setFormalTypeParameters("<E>").build();
    // assertSame(IDecl.Kind.CLASS, p.getKind());
    // assertEquals("<E>", p.getFormalTypeParameters());

    try {
      p = new Decl.ClassBuilder("111").build();
      fail("111 was a legal class name");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try { // abstract and final class
      p = new Decl.ClassBuilder("Foo").setIsAbstract(true).setIsFinal(true).build();
      fail("Foo was allowed to be both abstract and final");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testConstructorBuilder() {
    // java.lang.Object
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");
    // java.lang.String
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));

    Decl.ConstructorBuilder b = new Decl.ConstructorBuilder();
    // parameters: (Object, Object, String)
    // b.addParameter(jlo);
    // b.addParameter(jlo);
    // b.addParameter(string);
    b.setParent(parent);
    IDecl p = b.build();

    assertSame(IDecl.Kind.CONSTRUCTOR, p.getKind());
    assertEquals("MyType", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    IDecl[] paramaterTypes = p.getParameters();
    assertEquals(3, paramaterTypes.length);
    assertEquals(jlo, paramaterTypes[0]);
    assertEquals(jlo, paramaterTypes[1]);
    assertEquals(string, paramaterTypes[2]);
    assertNull(p.getTypeOf());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());
  }

  public void testEnumBuilder() {
    IDecl p = new Decl.EnumBuilder("Foo").build();
    assertSame(IDecl.Kind.ENUM, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    assertEquals(0, p.getParameters().length);
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    p = new Decl.EnumBuilder("Foo").setVisibility(Visibility.DEFAULT).build();
    assertSame(IDecl.Kind.ENUM, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.DEFAULT, p.getVisiblity());

    try {
      p = new Decl.EnumBuilder("111").build();
      fail("111 was a legal enum name");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testFieldBuilder() {
    // java.lang.Object
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");

    Decl.FieldBuilder b = new Decl.FieldBuilder("f_field");
    b.setTypeOf(jlo);

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));

    b.setParent(parent);
    b.setVisibility(Visibility.PRIVATE);
    IDecl p = b.build();

    assertSame(IDecl.Kind.FIELD, p.getKind());
    assertEquals("f_field", p.getName());
    assertSame(Visibility.PRIVATE, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(jlo, p.getTypeOf());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    assertEquals(0, p.getParameters().length);
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    p = new Decl.FieldBuilder("f_field2").setIsFinal(true).setIsStatic(true).setParent(parent).setTypeOf(jlo).build();
    assertTrue(p.isFinal());
    assertTrue(p.isStatic());

    try {
      p = new Decl.FieldBuilder("111").setParent(parent).setTypeOf(jlo).build();
      fail("111 was a legal field name");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testInitializerBuilder() {
    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));

    Decl.InitializerBuilder b = new Decl.InitializerBuilder();
    b.setParent(parent);
    IDecl p = b.build();
    assertSame(IDecl.Kind.INITIALIZER, p.getKind());
    assertEquals("", p.getName());
    assertSame(Visibility.NA, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertNull(p.getTypeOf());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    assertEquals(0, p.getParameters().length);
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    p = new Decl.InitializerBuilder().setParent(parent).setIsStatic(true).build();
    assertTrue(p.isStatic());
  }

  public void testInterfaceBuilder() {
    IDecl p = new Decl.InterfaceBuilder("Foo").build();
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    assertEquals(0, p.getParameters().length);
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    Decl.InterfaceBuilder inner = new Decl.InterfaceBuilder("Inner");
    Decl.InterfaceBuilder outer = new Decl.InterfaceBuilder("Outer");
    Decl.PackageBuilder pkg = new Decl.PackageBuilder("org.apache");
    outer.setParent(pkg);
    inner.setParent(outer);
    inner.setVisibility(Visibility.PRIVATE);
    p = inner.build();
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Inner", p.getName());
    assertSame(Visibility.PRIVATE, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    p = p.getParent();
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Outer", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("apache", p.getName());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("org", p.getName());
    assertNull(p.getParent());

    // TODO
    // p = new
    // Decl.InterfaceBuilder("Foo").setFormalTypeParameters("<E>").build();
    // assertSame(IDecl.Kind.INTERFACE, p.getKind());
    // assertEquals("<E>", p.getFormalTypeParameters());

    try {
      p = new Decl.InterfaceBuilder("111").build();
      fail("111 was a legal class name");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testMethodBuilder() {
    // java.lang.Object
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");
    // java.lang.String
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, Object, String)
    // b.addParameter(jlo);
    // b.addParameter(jlo);
    // b.addParameter(string);
    b.setParent(parent);
    IDecl p = b.build();

    assertSame(IDecl.Kind.METHOD, p.getKind());
    assertEquals("processSomething", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    IDecl[] paramaterTypes = p.getParameters();
    assertEquals(3, paramaterTypes.length);
    assertEquals(jlo, paramaterTypes[0]);
    assertEquals(jlo, paramaterTypes[1]);
    assertEquals(string, paramaterTypes[2]);
    assertNull(p.getTypeOf());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    // TODO
    // p = new
    // Decl.MethodBuilder("Foo").setParent(parent).setFormalTypeParameters("<E>").build();
    // assertSame(IDecl.Kind.METHOD, p.getKind());
    // assertEquals("<E>", p.getFormalTypeParameters());

    try {
      p = new Decl.MethodBuilder("111").setParent(parent).build();
      fail("111 was a legal method name");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.MethodBuilder("Foo").setParent(parent).setIsAbstract(true).setIsFinal(true).build();
      fail("Foo was allowed to be both abstract and final");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testPackageBuilder() {
    IDecl p = new Decl.PackageBuilder(null).build();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getName());
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("").build();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getName());
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("solo").build();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("solo", p.getName());
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("com.surelogic").build();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("surelogic", p.getName());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("com", p.getName());
    assertNull(p.getParent());

    Decl.PackageBuilder surelogic = new Decl.PackageBuilder("surelogic");
    Decl.PackageBuilder com = new Decl.PackageBuilder("com");
    surelogic.setParent(com);
    p = surelogic.build();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("surelogic", p.getName());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("com", p.getName());
    assertNull(p.getParent());

    Decl.PackageBuilder oa = new Decl.PackageBuilder("org.apache");
    Decl.PackageBuilder stupidParent = new Decl.PackageBuilder(null);
    oa.setParent(stupidParent);
    p = oa.build(); // stupid parent should be ignored
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("apache", p.getName());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("org", p.getName());
    assertNull(p.getParent());

    try { // empty nested packages
      p = new Decl.PackageBuilder("...").build();
      fail("... was a legal package");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.PackageBuilder("com.surelogic.333.edu").build();
      fail("package name 333 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try { // parent to the default package
      Decl.PackageBuilder defaultPkg = new Decl.PackageBuilder(null);
      Decl.PackageBuilder illegalParent = new Decl.PackageBuilder("com");
      defaultPkg.setParent(illegalParent);
      p = defaultPkg.build();
      fail("parent to the default package was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testParameterBuilder() {
    // java.lang.Object
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");
    // java.lang.String
    TypeRef string = new TypeRef("java.lang.String", "String");
    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, Object, String)
//    b.addParameter(jlo);
//    b.addParameter(jlo);
//    b.addParameter(string);
    b.setParent(parent);

    Decl.ParameterBuilder param = new Decl.ParameterBuilder(0, "foo");
    param.setTypeOf(jlo);
    param.setParent(b);
    IDecl p = param.build();
    assertSame(IDecl.Kind.PARAMETER, p.getKind());
    assertEquals("foo", p.getName());
    assertSame(Visibility.NA, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(Decl.EMPTY, p.getTypeParameters());
    assertEquals(0, p.getParameters().length);
    assertEquals(jlo, p.getTypeOf());
    assertEquals("processSomething", p.getParent().getName());
    assertEquals("MyType", p.getParent().getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent().getParent());

    p = new Decl.ParameterBuilder(0).setParent(b).setTypeOf(jlo).setIsFinal(true).build();
    assertTrue(p.isFinal());

    try {
      new Decl.ParameterBuilder(1, "111").setParent(b).setTypeOf(jlo).build();
      fail("111 was a legal parameter name");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      new Decl.ParameterBuilder(-1).setParent(b).setTypeOf(jlo).build();
      fail("argnum of -1 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      new Decl.ParameterBuilder(255).setParent(b).setTypeOf(jlo).build();
      fail("argnum of 255 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }
}
