package com.surelogic.common.ref;

import java.util.List;

import junit.framework.TestCase;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IDecl.Visibility;

public class TestDecl extends TestCase {

  public void testClassBuilder() {
    IDecl p = new Decl.ClassBuilder("Foo").setParent(new Decl.PackageBuilder()).build();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    Decl.ClassBuilder inner = new Decl.ClassBuilder("Inner");
    Decl.ClassBuilder outer = new Decl.ClassBuilder("Outer");
    Decl.PackageBuilder pkg = new Decl.PackageBuilder("apache").setParent(new Decl.PackageBuilder("org"));
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

    Decl.ClassBuilder foo = new Decl.ClassBuilder("Foo");
    foo.setParent(new Decl.PackageBuilder());
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    IDecl fooDecl = foo.build();
    assertEquals(1, fooDecl.getTypeParameters().size());
    assertEquals("E", fooDecl.getTypeParameters().get(0).getName());

    try {
      p = new Decl.ClassBuilder("Foo").build();
      fail("Foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      p = new Decl.ClassBuilder("111").build();
      fail("111 was a legal class name");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      p = new Decl.ClassBuilder("Foo").setParent(new Decl.PackageBuilder()).setIsAbstract(true).setIsFinal(true).build();
      fail("Foo was allowed to be both abstract and final");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testConstructorBuilder() {
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));

    Decl.ConstructorBuilder b = new Decl.ConstructorBuilder();
    // parameters: (Object, Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(jlo));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(jlo));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setParent(parent);
    IDecl p = b.build();

    assertSame(IDecl.Kind.CONSTRUCTOR, p.getKind());
    assertEquals("MyType", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(0, p.getTypeParameters().size());
    List<IDecl> parameters = p.getParameters();
    assertEquals(3, parameters.size());
    assertEquals(jlo, parameters.get(0).getTypeOf());
    assertEquals(jlo, parameters.get(1).getTypeOf());
    assertEquals(string, parameters.get(2).getTypeOf());
    assertNull(p.getTypeOf());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));
    Decl.ConstructorBuilder foo = new Decl.ConstructorBuilder();
    foo.setParent(parent);
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    IDecl fooDecl = foo.build();
    assertEquals(1, fooDecl.getTypeParameters().size());
    assertEquals("E", fooDecl.getTypeParameters().get(0).getName());

    try {
      p = new Decl.ConstructorBuilder().build();
      fail("constructor allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testEnumBuilder() {
    IDecl p = new Decl.EnumBuilder("Foo").setParent(new Decl.PackageBuilder()).build();
    assertSame(IDecl.Kind.ENUM, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    p = new Decl.EnumBuilder("Foo").setParent(new Decl.PackageBuilder()).setVisibility(Visibility.DEFAULT).build();
    assertSame(IDecl.Kind.ENUM, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.DEFAULT, p.getVisiblity());

    try {
      p = new Decl.EnumBuilder("111").build();
      fail("111 was a legal enum name");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.EnumBuilder("Foo").build();
      fail("Foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testFieldBuilder() {
    // java.lang.Object
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");

    Decl.FieldBuilder b = new Decl.FieldBuilder("f_field");
    b.setTypeOf(jlo);

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));

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
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
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
    try {
      p = new Decl.FieldBuilder("foo").setParent(parent).build();
      fail("typeOf allowed to be null");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      p = new Decl.FieldBuilder("foo").setTypeOf(jlo).build();
      fail("foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testInitializerBuilder() {
    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));

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
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    p = new Decl.InitializerBuilder().setParent(parent).setIsStatic(true).build();
    assertTrue(p.isStatic());

    try {
      p = new Decl.InitializerBuilder().build();
      fail("initializer allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testInterfaceBuilder() {
    IDecl p = new Decl.InterfaceBuilder("Foo").setParent(new Decl.PackageBuilder()).build();
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertTrue(p.getTypeParameters().isEmpty());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    Decl.InterfaceBuilder inner = new Decl.InterfaceBuilder("Inner");
    Decl.InterfaceBuilder outer = new Decl.InterfaceBuilder("Outer");
    Decl.PackageBuilder pkg = new Decl.PackageBuilder("apache").setParent(new Decl.PackageBuilder("org"));
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

    Decl.InterfaceBuilder foo = new Decl.InterfaceBuilder("Foo");
    foo.setParent(new Decl.PackageBuilder());
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    IDecl fooDecl = foo.build();
    assertEquals(1, fooDecl.getTypeParameters().size());
    assertEquals("E", fooDecl.getTypeParameters().get(0).getName());

    try {
      p = new Decl.InterfaceBuilder("Foo").build();
      fail("Foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.InterfaceBuilder("111").build();
      fail("111 was a legal class name");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testMethodBuilder() {
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(jlo));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(jlo));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setParent(parent);
    IDecl p = b.build();

    assertSame(IDecl.Kind.METHOD, p.getKind());
    assertEquals("processSomething", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(0, p.getTypeParameters().size());
    List<IDecl> parameters = p.getParameters();
    assertEquals(3, parameters.size());
    assertEquals(jlo, parameters.get(0).getTypeOf());
    assertEquals(jlo, parameters.get(1).getTypeOf());
    assertEquals(string, parameters.get(2).getTypeOf());
    assertNull(p.getTypeOf());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));
    Decl.MethodBuilder foo = new Decl.MethodBuilder("testParamType");
    foo.setParent(parent);
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    IDecl fooDecl = foo.build();
    assertEquals(1, fooDecl.getTypeParameters().size());
    assertEquals("E", fooDecl.getTypeParameters().get(0).getName());

    try {
      p = new Decl.MethodBuilder("foo").build();
      fail("foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }

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
    IDecl p = new Decl.PackageBuilder().build();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getName());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, DeclUtil.getPackageName(p));
    assertNull(DeclUtil.getPackageNameOrNull(p));
    assertEquals("", DeclUtil.getPackageNameSlash(p));
    assertNull(p.getParent());

    p = new Decl.PackageBuilder(null).build();
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
    assertEquals("solo", DeclUtil.getPackageName(p));
    assertEquals("solo", DeclUtil.getPackageNameOrNull(p));
    assertEquals("solo", DeclUtil.getPackageNameSlash(p));
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("surelogic").setParent(new Decl.PackageBuilder("com")).build();
    assertEquals("com.surelogic", DeclUtil.getPackageName(p));
    assertEquals("com.surelogic", DeclUtil.getPackageNameOrNull(p));
    assertEquals("com/surelogic", DeclUtil.getPackageNameSlash(p));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("surelogic", p.getName());
    p = p.getParent();
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("com", p.getName());
    assertNull(p.getParent());

    try {
      p = new Decl.PackageBuilder("333").build();
      fail("333 was a legal package");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
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

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(jlo));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(jlo));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setParent(parent);

    Decl.ParameterBuilder param = new Decl.ParameterBuilder(3, "foo");
    param.setTypeOf(jlo);
    param.setParent(b);
    IDecl p = param.build();
    assertSame(IDecl.Kind.PARAMETER, p.getKind());
    assertEquals("foo", p.getName());
    assertSame(Visibility.NA, p.getVisiblity());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertEquals(jlo, p.getTypeOf());
    assertEquals("processSomething", p.getParent().getName());
    assertEquals("MyType", p.getParent().getParent().getName());
    assertEquals("surelogic", p.getParent().getParent().getParent().getName());
    assertEquals("com", p.getParent().getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent().getParent());

    p = new Decl.ParameterBuilder(4).setParent(b).setTypeOf(jlo).setIsFinal(true).build();
    assertTrue(p.isFinal());

    try {
      p = new Decl.ParameterBuilder(0, "foo").setTypeOf(jlo).build();
      fail("foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "surelogic").setParent(new Decl.PackageBuilder("com"))));
      p = new Decl.ParameterBuilder(0, "foo").setParent(b).build();
      fail("typeOf allowed to be null");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "surelogic").setParent(new Decl.PackageBuilder("com"))));
      new Decl.ParameterBuilder(1, "111").setParent(b).setTypeOf(jlo).build();
      fail("111 was a legal parameter name");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "surelogic").setParent(new Decl.PackageBuilder("com"))));
      new Decl.ParameterBuilder(-1).setParent(b).setTypeOf(jlo).build();
      fail("argnum of -1 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "surelogic").setParent(new Decl.PackageBuilder("com"))));
      new Decl.ParameterBuilder(255).setParent(b).setTypeOf(jlo).build();
      fail("argnum of 255 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "surelogic").setParent(new Decl.PackageBuilder("com"))));
      b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(jlo));
      new Decl.ParameterBuilder(0).setParent(b).setTypeOf(jlo).build();
      fail("(method) two parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      Decl.ConstructorBuilder cb = new Decl.ConstructorBuilder().setParent(new Decl.ClassBuilder("MyType")
          .setParent(new Decl.PackageBuilder("surelogic").setParent(new Decl.PackageBuilder("com"))));
      cb.addParameter(new Decl.ParameterBuilder(0).setTypeOf(jlo));
      new Decl.ParameterBuilder(0).setParent(cb).setTypeOf(jlo).build();
      fail("(constructor) two parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testTypeParameterBuilder() {
    TypeRef jlo = new TypeRef("java.lang.Object", "Object");
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
        .setParent(new Decl.PackageBuilder("com")));

    parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
    parent.addTypeParameter(new Decl.TypeParameterBuilder(1, "T").addBounds(jlo).addBounds(string));

    IDecl p = parent.build();
    assertEquals(2, p.getTypeParameters().size());
    IDecl tp1 = p.getTypeParameters().get(0);
    assertEquals("E", tp1.getName());
    assertTrue(tp1.getBounds().isEmpty());
    IDecl tp2 = p.getTypeParameters().get(1);
    assertEquals("T", tp2.getName());
    List<TypeRef> bounds = tp2.getBounds();
    assertEquals(2, bounds.size());
    assertEquals(jlo.getFullyQualified(), bounds.get(0).getFullyQualified());
    assertEquals(string.getFullyQualified(), bounds.get(1).getFullyQualified());

    try {
      parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic").setParent(new Decl.PackageBuilder(
          "com")));
      parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "T").addBounds(jlo).addBounds(string));
      p = parent.build();
      fail("(class) two type parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      Decl.InterfaceBuilder iparent = new Decl.InterfaceBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic")
          .setParent(new Decl.PackageBuilder("com")));
      iparent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      iparent.addTypeParameter(new Decl.TypeParameterBuilder(0, "T").addBounds(jlo).addBounds(string));
      p = iparent.build();
      fail("(interface) two type parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("surelogic").setParent(new Decl.PackageBuilder(
          "com")));
      Decl.MethodBuilder method = new Decl.MethodBuilder("processSomething").setParent(parent);
      method.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      method.addTypeParameter(new Decl.TypeParameterBuilder(0, "T").addBounds(jlo).addBounds(string));
      p = method.build();
      fail("(method) two type parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }
}
