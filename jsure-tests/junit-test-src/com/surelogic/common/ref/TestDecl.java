package com.surelogic.common.ref;

import java.util.List;

import junit.framework.TestCase;

import com.surelogic.common.SLUtility;
import com.surelogic.common.StringCache;
import com.surelogic.common.ref.IDecl.Visibility;

public class TestDecl extends TestCase {

  public void testBadDoubleCallToBuild() {
    Decl.DeclBuilder b = new Decl.PackageBuilder();
    b.build();
    try {
      b.build();
      fail("build invoked twice with no IllegalStateException thrown");
    } catch (IllegalStateException expected) {
      // good
    }
  }

  public void testClassBuilder() {
    IDecl p = new Decl.ClassBuilder("Foo").setParent(new Decl.PackageBuilder()).build();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Foo", p.getName());
    assertEquals("Foo", DeclUtil.getTypeNameOrNull(p));
    assertEquals("Foo", DeclUtil.getTypeNameDollarSignOrNull(p));
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));

    Decl.ClassBuilder inner = new Decl.ClassBuilder("Inner");
    Decl.ClassBuilder outer = new Decl.ClassBuilder("Outer");
    Decl.PackageBuilder pkg = new Decl.PackageBuilder("org.apache");
    outer.setParent(pkg);
    inner.setParent(outer);
    inner.setVisibility(Visibility.PRIVATE);
    inner.setIsStatic(true);
    outer.setIsFinal(true);
    p = inner.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Inner", p.getName());
    assertEquals("Outer.Inner", DeclUtil.getTypeNameOrNull(p));
    assertEquals("Outer$Inner", DeclUtil.getTypeNameDollarSignOrNull(p));
    assertEquals("org.apache.Outer", DeclUtil.getTypeNameFullyQualifiedOutermostTypeNameOnly(p));
    assertSame(Visibility.PRIVATE, p.getVisibility());
    assertFalse(p.isAbstract());
    assertTrue(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    p = p.getParent();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Outer", p.getName());
    assertEquals("Outer", DeclUtil.getTypeNameOrNull(p));
    assertEquals("Outer", DeclUtil.getTypeNameDollarSignOrNull(p));
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertTrue(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    p = p.getParent();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("org.apache", p.getName());
    assertNull(DeclUtil.getTypeNameOrNull(p));
    assertNull(DeclUtil.getTypeNameDollarSignOrNull(p));
    assertNull(p.getParent());

    Decl.ClassBuilder anon = new Decl.ClassBuilder("ShouldBeIgnored");
    anon.setVisibility(Visibility.ANONYMOUS);
    anon.setParent(new Decl.ClassBuilder("Outer").setParent(new Decl.PackageBuilder()));
    anon.setAnonymousDeclPosition(2);
    p = anon.build();
    assertSame(Visibility.ANONYMOUS, p.getVisibility());
    assertEquals(2, p.getPosition());
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());

    Decl.ClassBuilder foo = new Decl.ClassBuilder("Foo");
    foo.setParent(new Decl.PackageBuilder());
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    IDecl fooDecl = foo.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(fooDecl));
    assertSame(fooDecl.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(fooDecl), Decl.encodeForPersistence(pEncode));

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
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));

    Decl.ConstructorBuilder b = new Decl.ConstructorBuilder();
    // parameters: (Object, Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setParent(parent);
    IDecl p = b.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.CONSTRUCTOR, p.getKind());
    assertEquals("MyType", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertEquals(0, p.getTypeParameters().size());
    List<IDeclParameter> parameters = p.getParameters();
    assertEquals(3, parameters.size());
    assertEquals(TypeRef.JAVA_LANG_OBJECT, parameters.get(0).getTypeOf());
    assertEquals(TypeRef.JAVA_LANG_OBJECT, parameters.get(1).getTypeOf());
    assertEquals(string, parameters.get(2).getTypeOf());
    assertNull(p.getTypeOf());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent());

    parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));
    b = new Decl.ConstructorBuilder();
    b.setParent(parent);
    b.setIsImplicit(true);
    p = b.build();
    assertTrue(p.isImplicit());
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(pEncode.isImplicit());

    parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic"));
    Decl.ConstructorBuilder foo = new Decl.ConstructorBuilder();
    foo.setParent(parent);
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    Decl.TypeParameterBuilder tpb0 = foo.getTypeParameterBuilderAt(0);
    assertSame(tpb, tpb0);
    IDecl fooDecl = foo.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(fooDecl));
    assertSame(fooDecl.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(fooDecl), Decl.encodeForPersistence(pEncode));
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
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    p = new Decl.EnumBuilder("Foo").setParent(new Decl.PackageBuilder()).setVisibility(Visibility.DEFAULT).build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.ENUM, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.DEFAULT, p.getVisibility());

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
    Decl.FieldBuilder b = new Decl.FieldBuilder("f_field");
    b.setTypeOf(TypeRef.JAVA_LANG_OBJECT);

    b.setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")));
    b.setVisibility(Visibility.PRIVATE);
    IDecl p = b.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.FIELD, p.getKind());
    assertEquals("f_field", p.getName());
    assertSame(Visibility.PRIVATE, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertEquals(TypeRef.JAVA_LANG_OBJECT, p.getTypeOf());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent());

    p = new Decl.FieldBuilder("f_field2").setIsFinal(true).setIsStatic(true).setIsVolatile(true)
        .setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")))
        .setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
    assertTrue(p.isFinal());
    assertTrue(p.isStatic());
    assertTrue(p.isVolatile());
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());

    try {
      p = new Decl.FieldBuilder("111")
          .setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")))
          .setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("111 was a legal field name");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      p = new Decl.FieldBuilder("foo").setParent(
          new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"))).build();
      fail("typeOf allowed to be null");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      p = new Decl.FieldBuilder("foo").setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testInitializerBuilder() {

    Decl.InitializerBuilder b = new Decl.InitializerBuilder();
    b.setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")));
    IDecl p = b.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.INITIALIZER, p.getKind());
    assertEquals("", p.getName());
    assertSame(Visibility.NA, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertNull(p.getTypeOf());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent());

    p = new Decl.InitializerBuilder()
        .setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"))).setIsStatic(true).build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
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
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Foo", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertTrue(p.getTypeParameters().isEmpty());
    assertEquals(0, p.getParameters().size());
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
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Inner", p.getName());
    assertSame(Visibility.PRIVATE, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    p = p.getParent();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.INTERFACE, p.getKind());
    assertEquals("Outer", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    p = p.getParent();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("org.apache", p.getName());
    assertNull(p.getParent());

    Decl.InterfaceBuilder foo = new Decl.InterfaceBuilder("Foo");
    foo.setParent(new Decl.PackageBuilder());
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    Decl.TypeParameterBuilder tpb0 = foo.getTypeParameterBuilderAt(0);
    assertSame(tpb, tpb0);
    IDecl fooDecl = foo.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(fooDecl));
    assertSame(fooDecl.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(fooDecl), Decl.encodeForPersistence(pEncode));
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

  public void testAnnotationBuilder() {
    IDecl p = new Decl.AnnotationBuilder("ThreadSafe").setParent(new Decl.PackageBuilder()).build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.ANNOTATION, p.getKind());
    assertEquals("ThreadSafe", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertTrue(p.getTypeParameters().isEmpty());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());

    Decl.AnnotationBuilder inner = new Decl.AnnotationBuilder("Inner");
    Decl.AnnotationBuilder outer = new Decl.AnnotationBuilder("Outer");
    Decl.PackageBuilder pkg = new Decl.PackageBuilder("org.apache");
    outer.setParent(pkg);
    inner.setParent(outer);
    inner.setVisibility(Visibility.PRIVATE);
    p = inner.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.ANNOTATION, p.getKind());
    assertEquals("Inner", p.getName());
    assertSame(Visibility.PRIVATE, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    p = p.getParent();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.ANNOTATION, p.getKind());
    assertEquals("Outer", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    p = p.getParent();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("org.apache", p.getName());
    assertNull(p.getParent());

    try {
      p = new Decl.AnnotationBuilder("Foo").build();
      fail("Foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.AnnotationBuilder("111").build();
      fail("111 was a legal class name");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testLambdaBuilder() {
    TypeRef string = new TypeRef("java.lang.String", "String");
    TypeRef runnable = new TypeRef("java.lang.Runnable", "Runnable");

    Decl.MethodBuilder m = new Decl.MethodBuilder("processSomething");
    m.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    m.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    m.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    m.setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")));

    Decl.LambdaBuilder b = new Decl.LambdaBuilder();
    b.addParameter(new Decl.ParameterBuilder(0, "a").setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(string));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.setParent(m);

    IDecl p = b.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.LAMBDA, p.getKind());
    assertEquals("", p.getName());
    assertTrue(p.getTypeParameters().isEmpty());
    assertEquals(3, p.getParameters().size());
    assertEquals(p.getParameters(), pEncode.getParameters());
    assertEquals(runnable, p.getTypeOf());
    assertEquals("processSomething", p.getParent().getName());
    assertEquals("MyType", p.getParent().getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    b = new Decl.LambdaBuilder();
    m = new Decl.MethodBuilder("processSomething");
    m.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    m.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    m.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    m.setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")));
    b.setParent(m);
    b.setFunctionalInterfaceTypeOf(string);
    b.setDeclPosition(5);
    p = b.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.LAMBDA, p.getKind());
    assertEquals("", p.getName());
    assertTrue(p.getTypeParameters().isEmpty());
    assertEquals(0, p.getParameters().size());
    assertEquals(string, p.getTypeOf());
    assertEquals(5, p.getPosition());
  }

  public void testMethodBuilder() {
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder classParent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
    Decl.InterfaceBuilder interfaceParent = new Decl.InterfaceBuilder("MyInt")
        .setParent(new Decl.PackageBuilder("com.surelogic.t"));

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setParent(classParent);
    IDecl p = b.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.METHOD, p.getKind());
    assertEquals("processSomething", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertFalse(p.isDefault());
    assertEquals(0, p.getTypeParameters().size());
    List<IDeclParameter> parameters = p.getParameters();
    assertEquals(3, parameters.size());
    assertEquals(TypeRef.JAVA_LANG_OBJECT, parameters.get(0).getTypeOf());
    assertEquals(TypeRef.JAVA_LANG_OBJECT, parameters.get(1).getTypeOf());
    assertEquals(string, parameters.get(2).getTypeOf());
    assertNull(p.getTypeOf());
    assertEquals("MyType", p.getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent());

    b = new Decl.MethodBuilder("defaultSomething");
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setParent(interfaceParent);
    b.setIsDefault(true);
    b.setReturnTypeOf(TypeRef.JAVA_LANG_RUNNABLE);
    p = b.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.METHOD, p.getKind());
    assertEquals("defaultSomething", p.getName());
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertTrue(p.isDefault());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(3, p.getParameters().size());
    assertEquals(TypeRef.JAVA_LANG_RUNNABLE, p.getTypeOf());
    assertEquals("MyInt", p.getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent());

    classParent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
    Decl.MethodBuilder foo = new Decl.MethodBuilder("testParamType");
    foo.setParent(classParent);
    Decl.TypeParameterBuilder tpb = new Decl.TypeParameterBuilder(0, "E");
    foo.addTypeParameter(tpb);
    Decl.TypeParameterBuilder tpb0 = foo.getTypeParameterBuilderAt(0);
    assertSame(tpb, tpb0);
    foo.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    foo.addParameter(new Decl.ParameterBuilder(1).setTypeOf(string));
    IDecl fooDecl = foo.build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(fooDecl));
    assertTrue(fooDecl.hasSameAttributesAs(pEncode));
    assertTrue(fooDecl.isSameSimpleDeclarationAs(pEncode));
    assertTrue(fooDecl.isSameDeclarationAs(pEncode));
    assertTrue(fooDecl.equals(pEncode));
    assertEquals(fooDecl.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(fooDecl).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(fooDecl).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(fooDecl.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(fooDecl), Decl.encodeForPersistence(pEncode));
    assertEquals(1, fooDecl.getTypeParameters().size());
    assertEquals("E", fooDecl.getTypeParameters().get(0).getName());

    try {
      p = new Decl.MethodBuilder("foo").build();
      fail("foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      classParent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      new Decl.MethodBuilder("111").setParent(classParent).build();
      fail("111 was a legal method name");
    } catch (IllegalArgumentException e1) {
      // good
    }

    try {
      classParent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      new Decl.MethodBuilder("Foo").setParent(classParent).setIsAbstract(true).setIsFinal(true).build();
      fail("Foo was allowed to be both abstract and final");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      classParent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      new Decl.MethodBuilder("Foo").setParent(classParent).setIsDefault(true).build();
      fail("Foo was allowed to be default within a class");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testPackageBuilder() {
    IDecl p = new Decl.PackageBuilder().build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getName());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, DeclUtil.getPackageName(p));
    assertNull(DeclUtil.getPackageNameOrNull(p));
    assertEquals("", DeclUtil.getPackageNameSlash(p));
    assertNull(p.getParent());

    p = new Decl.PackageBuilder(null).build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getName());
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("").build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getName());
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("solo").build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("solo", p.getName());
    assertEquals("solo", DeclUtil.getPackageName(p));
    assertEquals("solo", DeclUtil.getPackageNameOrNull(p));
    assertEquals("solo", DeclUtil.getPackageNameSlash(p));
    assertNull(p.getParent());

    p = new Decl.PackageBuilder("com.surelogic").build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertEquals("com.surelogic", DeclUtil.getPackageName(p));
    assertEquals("com.surelogic", DeclUtil.getPackageNameOrNull(p));
    assertEquals("com/surelogic", DeclUtil.getPackageNameSlash(p));
    assertSame(IDecl.Kind.PACKAGE, p.getKind());
    assertEquals("com.surelogic", p.getName());
    assertNull(p.getParent());

    try {
      p = new Decl.PackageBuilder("apache").setParent(new Decl.PackageBuilder("org")).build();
      fail("package was allowed to have a non-null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      p = new Decl.PackageBuilder("333").build();
      fail("333 was a legal package");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.PackageBuilder("foo.333.bar").build();
      fail("foo.333.bar was a legal package");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.PackageBuilder("foo..bar").build();
      fail("foo..bar was a legal package");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.PackageBuilder(".bar").build();
      fail(".bar was a legal package");
    } catch (IllegalArgumentException expected) {
      // good
    }

    try {
      p = new Decl.PackageBuilder("foo.").build();
      fail("foo. was a legal package");
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
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    Decl.ParameterBuilder p0 = new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT);
    Decl.ParameterBuilder p1 = new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT);
    Decl.ParameterBuilder p2 = new Decl.ParameterBuilder(2).setTypeOf(string);
    b.addParameter(p0);
    b.addParameter(p1);
    b.addParameter(p2);
    b.setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")));
    Decl.ParameterBuilder p3 = new Decl.ParameterBuilder(3, "foo");
    p3.setTypeOf(TypeRef.JAVA_LANG_OBJECT);
    p3.setParent(b);
    assertSame(p0, b.getParameterBuilderAt(0));
    assertSame(p1, b.getParameterBuilderAt(1));
    assertSame(p2, b.getParameterBuilderAt(2));
    assertSame(p3, b.getParameterBuilderAt(3));
    IDecl p = p3.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertTrue(SloppyWrapper.getInstance(p).equals(SloppyWrapper.getInstance(pEncode)));
    assertEquals(SloppyWrapper.getInstance(p).hashCode(), SloppyWrapper.getInstance(pEncode).hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertSame(IDecl.Kind.PARAMETER, p.getKind());
    assertEquals("foo", p.getName());
    assertSame(Visibility.NA, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertEquals(TypeRef.JAVA_LANG_OBJECT, p.getTypeOf());
    assertEquals("processSomething", p.getParent().getName());
    assertEquals("MyType", p.getParent().getParent().getName());
    assertEquals("com.surelogic.t", p.getParent().getParent().getParent().getName());
    assertNull(p.getParent().getParent().getParent().getParent());

    b = new Decl.MethodBuilder("processSomething");
    p0 = new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT);
    p1 = new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT);
    p2 = new Decl.ParameterBuilder(2).setTypeOf(string);
    b.addParameter(p0);
    b.addParameter(p1);
    b.addParameter(p2);
    b.setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t")));
    p3 = new Decl.ParameterBuilder(3, "foo");
    p3.setTypeOf(TypeRef.JAVA_LANG_OBJECT);
    p3.setParent(b);
    assertSame(p0, b.getParameterBuilderAt(0));
    assertSame(p1, b.getParameterBuilderAt(1));
    assertSame(p2, b.getParameterBuilderAt(2));
    assertSame(p3, b.getParameterBuilderAt(3));
    p = new Decl.ParameterBuilder(4).setParent(b).setTypeOf(TypeRef.JAVA_LANG_OBJECT).setIsFinal(true).build();
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertTrue(p.isFinal());

    try {
      p = new Decl.ParameterBuilder(0, "foo").setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("foo allowed to have a null parent");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "com.surelogic.t")));
      p = new Decl.ParameterBuilder(0, "foo").setParent(b).build();
      fail("typeOf allowed to be null");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "com.surelogic.t")));
      new Decl.ParameterBuilder(1, "111").setParent(b).setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("111 was a legal parameter name");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "com.surelogic.t")));
      new Decl.ParameterBuilder(-1).setParent(b).setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("argnum of -1 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "com.surelogic.t")));
      new Decl.ParameterBuilder(255).setParent(b).setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("argnum of 255 was allowed");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      b = new Decl.MethodBuilder("processSomething").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
          "surelogic").setParent(new Decl.PackageBuilder("com"))));
      b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
      new Decl.ParameterBuilder(0).setParent(b).setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("(method) two parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      Decl.ConstructorBuilder cb = new Decl.ConstructorBuilder().setParent(new Decl.ClassBuilder("MyType")
          .setParent(new Decl.PackageBuilder("surelogic").setParent(new Decl.PackageBuilder("com"))));
      cb.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
      new Decl.ParameterBuilder(0).setParent(cb).setTypeOf(TypeRef.JAVA_LANG_OBJECT).build();
      fail("(constructor) two parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      Decl.ConstructorBuilder cb = new Decl.ConstructorBuilder().setParent(new Decl.ClassBuilder("MyType")
          .setParent(new Decl.PackageBuilder("surelogic").setParent(new Decl.PackageBuilder("com"))));
      cb.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
      cb.getParameterBuilderAt(4);
      fail("Got a parameter at position 4 when none existed");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testTypeParameterBuilder() {
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));

    parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
    // Object bounds should be ignored
    parent.addTypeParameter(new Decl.TypeParameterBuilder(1, "T").addBounds(TypeRef.JAVA_LANG_OBJECT).addBounds(string));

    IDecl p = parent.build();
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertTrue(p.hasSameAttributesAs(pEncode));
    assertTrue(p.isSameSimpleDeclarationAs(pEncode));
    assertTrue(p.isSameDeclarationAs(pEncode));
    assertTrue(p.equals(pEncode));
    assertEquals(p.hashCode(), pEncode.hashCode());
    assertSame(p.getKind(), pEncode.getKind());
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    assertEquals(2, p.getTypeParameters().size());
    IDecl tp1 = p.getTypeParameters().get(0);
    assertEquals("E", tp1.getName());
    assertTrue(tp1.getBounds().isEmpty());
    IDecl tp2 = p.getTypeParameters().get(1);
    assertEquals("T", tp2.getName());
    List<TypeRef> bounds = tp2.getBounds();
    assertEquals(1, bounds.size()); // Object ignored
    assertEquals(string.getFullyQualified(), bounds.get(0).getFullyQualified());

    try {
      parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "T").addBounds(TypeRef.JAVA_LANG_OBJECT).addBounds(string));
      p = parent.build();
      fail("(class) two type parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      Decl.InterfaceBuilder iparent = new Decl.InterfaceBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      iparent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      iparent.addTypeParameter(new Decl.TypeParameterBuilder(0, "T").addBounds(TypeRef.JAVA_LANG_OBJECT).addBounds(string));
      p = iparent.build();
      fail("(interface) two type parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      Decl.MethodBuilder method = new Decl.MethodBuilder("processSomething").setParent(parent);
      method.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      method.addTypeParameter(new Decl.TypeParameterBuilder(0, "T").addBounds(TypeRef.JAVA_LANG_OBJECT).addBounds(string));
      p = method.build();
      fail("(method) two type parameters allowed at argument 0 position");
    } catch (IllegalArgumentException expected) {
      // good
    }
    try {
      parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
      parent.addTypeParameter(new Decl.TypeParameterBuilder(0, "E"));
      parent.getTypeParameterBuilderAt(4);
      fail("Got a type parameter at position 4 when none existed");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  public void testSloppyMatch() {
    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setVisibility(Visibility.DEFAULT);
    b.setIsStatic(true);
    b.setParent(parent);
    IDecl p1 = b.build();
    SloppyWrapper<IDecl> s1 = SloppyWrapper.getInstance(p1);

    parent = new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder("com.surelogic.t"));
    b = new Decl.MethodBuilder("processSomething");
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(1).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    b.addParameter(new Decl.ParameterBuilder(2).setTypeOf(string));
    b.setVisibility(Visibility.PUBLIC);
    b.setIsFinal(true);
    b.setParent(parent);
    IDecl p2 = b.build();
    SloppyWrapper<IDecl> s2 = SloppyWrapper.getInstance(p2);

    // strict checks fail
    assertFalse(p1.equals(p2));
    assertTrue(p1.hashCode() != p2.hashCode());

    // sloppy checks pass
    assertTrue(s1.equals(s2));
    assertEquals(s1.hashCode(), s2.hashCode());
  }

  public void testVisitor() {
    ChattyDeclVisitor v;

    TypeRef string = new TypeRef("java.lang.String", "String");

    Decl.ClassBuilder parent = new Decl.ClassBuilder("Inner").setParent(new Decl.ClassBuilder("MyType")
        .setParent(new Decl.PackageBuilder("com.surelogic.t")));
    Decl.TypeParameterBuilder tparam = new Decl.TypeParameterBuilder(0, "E").setParent(parent);

    Decl.MethodBuilder b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    Decl.ParameterBuilder arg1 = new Decl.ParameterBuilder(1).setTypeOf(string);
    b.addParameter(arg1);
    b.setVisibility(Visibility.DEFAULT);
    b.setIsStatic(true);
    b.setParent(parent);
    IDecl p1 = b.build();
    v = new ChattyDeclVisitor();
    p1.acceptRootToThis(v);
    String oracle = "START visitPackage(com.surelogic.t) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> visitTypeParameter(E, partOfDecl=false) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitMethod(processSomething) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> visitParameters(count=2) -> visitParameter(arg0:Object,partOfDecl=false) -> visitParameter(arg1:String,partOfDecl=false) -> endVisitParameters(count=2) -> endVisitMethod(processSomething) -> END";
    assertEquals(oracle, v.getResult());

    v = new ChattyDeclVisitor();
    p1.acceptThisToRoot(v);
    oracle = "START visitMethod(processSomething) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> visitParameters(count=2) -> visitParameter(arg0:Object,partOfDecl=false) -> visitParameter(arg1:String,partOfDecl=false) -> endVisitParameters(count=2) -> endVisitMethod(processSomething) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> visitTypeParameter(E, partOfDecl=false) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitPackage(com.surelogic.t) -> END";
    assertEquals(oracle, v.getResult());

    v = new ChattyDeclVisitor();
    v.visitClassReturn = false;
    v.visitMethodReturn = false;
    p1.acceptRootToThis(v);
    oracle = "START visitPackage(com.surelogic.t) -> visitTypes(count=2) -> visitClass(MyType) -> endVisitClass(MyType) -> visitClass(Inner) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitMethod(processSomething) -> endVisitMethod(processSomething) -> END";
    assertEquals(oracle, v.getResult());

    v = new ChattyDeclVisitor();
    v.visitParametersReturn = false;
    v.visitTypeParametersReturn = false;
    p1.acceptThisToRoot(v);
    oracle = "START visitMethod(processSomething) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> visitParameters(count=2) -> endVisitParameters(count=2) -> endVisitMethod(processSomething) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitPackage(com.surelogic.t) -> END";
    assertEquals(oracle, v.getResult());

    v = new ChattyDeclVisitor();
    v.visitTypesReturn = false;
    v.visitMethodReturn = false;
    p1.acceptRootToThis(v);
    oracle = "START visitPackage(com.surelogic.t) -> visitTypes(count=2) -> endVisitTypes(count=2) -> visitMethod(processSomething) -> endVisitMethod(processSomething) -> END";
    assertEquals(oracle, v.getResult());

    parent = new Decl.ClassBuilder("Inner").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
        "com.surelogic.t")));
    tparam = new Decl.TypeParameterBuilder(0, "E").setParent(parent);
    IDecl tp = tparam.build();

    v = new ChattyDeclVisitor();
    tp.acceptRootToThis(v);
    oracle = "START visitPackage(com.surelogic.t) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> visitTypeParameter(E, partOfDecl=false) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitTypeParameter(E, partOfDecl=true) -> END";
    assertEquals(oracle, v.getResult());

    v = new ChattyDeclVisitor();
    v.visitTypeParametersReturn = false;
    v.visitParametersReturn = false;
    tp.acceptThisToRoot(v);
    oracle = "START visitTypeParameter(E, partOfDecl=true) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitPackage(com.surelogic.t) -> END";
    assertEquals(oracle, v.getResult());

    v = new ChattyDeclVisitor();
    tp.acceptRootToThis(v);
    oracle = "START visitPackage(com.surelogic.t) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> visitTypeParameter(E, partOfDecl=false) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitTypeParameter(E, partOfDecl=true) -> END";
    assertEquals(oracle, v.getResult());

    parent = new Decl.ClassBuilder("Inner").setParent(new Decl.ClassBuilder("MyType").setParent(new Decl.PackageBuilder(
        "com.surelogic.t")));
    tparam = new Decl.TypeParameterBuilder(0, "E").setParent(parent);

    b = new Decl.MethodBuilder("processSomething");
    // parameters: (Object, String)
    b.addParameter(new Decl.ParameterBuilder(0).setTypeOf(TypeRef.JAVA_LANG_OBJECT));
    arg1 = new Decl.ParameterBuilder(1).setTypeOf(string);
    b.addParameter(arg1);
    b.setVisibility(Visibility.DEFAULT);
    b.setIsStatic(true);
    b.setParent(parent);

    IDecl arg1decl = arg1.build();

    v = new ChattyDeclVisitor();
    v.visitTypeParametersReturn = false;
    v.visitParametersReturn = false;
    arg1decl.acceptThisToRoot(v);
    oracle = "START visitParameter(arg1:String,partOfDecl=true) -> visitMethod(processSomething) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> visitParameters(count=2) -> endVisitParameters(count=2) -> endVisitMethod(processSomething) -> visitTypes(count=2) -> visitClass(MyType) -> visitTypeParameters(count=0) -> endVisitTypeParameters(count=0) -> endVisitClass(MyType) -> visitClass(Inner) -> visitTypeParameters(count=1) -> endVisitTypeParameters(count=1) -> endVisitClass(Inner) -> endVisitTypes(count=2) -> visitPackage(com.surelogic.t) -> END";
    assertEquals(oracle, v.getResult());

    Decl.AnnotationBuilder anno = new Decl.AnnotationBuilder("ThreadSafe").setParent(new Decl.PackageBuilder("com.surelogic.t"));
    IDecl annoDecl = anno.build();
    v = new ChattyDeclVisitor();
    annoDecl.acceptRootToThis(v);
    oracle = "START visitPackage(com.surelogic.t) -> visitTypes(count=1) -> visitAnnotation(ThreadSafe) -> endVisitTypes(count=1) -> END";
    assertEquals(oracle, v.getResult());
  }

  public void testStringCache() {
    final StringCache cache = new StringCache();
    DeclUtil.setStringCache(cache);
    IDecl p = new Decl.ClassBuilder("Foo").setParent(new Decl.PackageBuilder()).build();
    assertSame(IDecl.Kind.CLASS, p.getKind());
    assertEquals("Foo", p.getName());
    assertEquals("Foo", DeclUtil.getTypeNameOrNull(p));
    assertEquals("Foo", DeclUtil.getTypeNameDollarSignOrNull(p));
    assertSame(Visibility.PUBLIC, p.getVisibility());
    assertFalse(p.isAbstract());
    assertFalse(p.isStatic());
    assertFalse(p.isFinal());
    assertFalse(p.isImplicit());
    assertFalse(p.isVolatile());
    assertEquals(0, p.getTypeParameters().size());
    assertEquals(0, p.getParameters().size());
    assertNull(p.getTypeOf());
    assertEquals(SLUtility.JAVA_DEFAULT_PACKAGE, p.getParent().getName());
    assertNull(p.getParent().getParent());
    IDecl pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertSame(p.getKind(), pEncode.getKind());
    assertSame(p.getName(), pEncode.getName()); // only if cache
    assertEquals(Decl.encodeForPersistence(p), Decl.encodeForPersistence(pEncode));
    DeclUtil.setStringCache(null);
    pEncode = Decl.parseEncodedForPersistence(Decl.encodeForPersistence(p));
    assertNotSame(p.getName(), pEncode.getName()); // no cache
  }
}
