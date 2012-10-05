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
    assertEquals("", p.getFormalTypeParameters());
    assertEquals(0, p.getFormalParameterTypes().length);
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

    p = new Decl.ClassBuilder("Foo").setFormalTypeParameters("<E>").build();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("<E>", p.getFormalTypeParameters());

    try { // empty nested packages
      p = new Decl.ClassBuilder("111").build();
      fail("111 was a legal class naem");
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

    try { // illegal package name
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
}
