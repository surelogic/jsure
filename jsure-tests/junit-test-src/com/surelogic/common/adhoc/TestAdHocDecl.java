package com.surelogic.common.adhoc;

import junit.framework.TestCase;

import com.surelogic.common.ref.IDecl;

public final class TestAdHocDecl extends TestCase {

  public void testDefaults() {
    IDecl d = new AdHocDecl(null);
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertFalse(d.isFinal());
    assertFalse(d.isImplicit());
    assertFalse(d.isStatic());
    assertFalse(d.isVolatile());
  }

  public void testDeclarationType() {
    IDecl d = new AdHocDecl("@AN");
    assertSame(IDecl.Kind.ANNOTATION, d.getKind());
    d = new AdHocDecl("@CL");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    d = new AdHocDecl("@CO");
    assertSame(IDecl.Kind.CONSTRUCTOR, d.getKind());
    d = new AdHocDecl("@EN");
    assertSame(IDecl.Kind.ENUM, d.getKind());
    d = new AdHocDecl("@FL");
    assertSame(IDecl.Kind.FIELD, d.getKind());
    d = new AdHocDecl("@IT");
    assertSame(IDecl.Kind.INITIALIZER, d.getKind());
    d = new AdHocDecl("@IN");
    assertSame(IDecl.Kind.INTERFACE, d.getKind());
    d = new AdHocDecl("@ME");
    assertSame(IDecl.Kind.METHOD, d.getKind());
    d = new AdHocDecl("@PK");
    assertSame(IDecl.Kind.PACKAGE, d.getKind());
    d = new AdHocDecl("@PA");
    assertSame(IDecl.Kind.PARAMETER, d.getKind());
    d = new AdHocDecl("@TP");
    assertSame(IDecl.Kind.TYPE_PARAMETER, d.getKind());
  }

  public void testVisibility() {
    IDecl d = new AdHocDecl("@CL");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    d = new AdHocDecl("@CL:");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    d = new AdHocDecl("@CL:AN");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.ANONYMOUS, d.getVisibility());
    d = new AdHocDecl("@CL:DE");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.DEFAULT, d.getVisibility());
    d = new AdHocDecl("@CL:PR");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PRIVATE, d.getVisibility());
    d = new AdHocDecl("@CL:PO");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PROTECTED, d.getVisibility());
    d = new AdHocDecl("@CL:PU");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    d = new AdHocDecl("@CL:NA");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.NA, d.getVisibility());
  }

  public void testModifiers() {
    IDecl d = new AdHocDecl("@CL:PU");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertFalse(d.isFinal());
    assertFalse(d.isImplicit());
    assertFalse(d.isStatic());
    assertFalse(d.isVolatile());

    d = new AdHocDecl("@CL:PU:");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertFalse(d.isFinal());
    assertFalse(d.isImplicit());
    assertFalse(d.isStatic());
    assertFalse(d.isVolatile());

    d = new AdHocDecl("@CL:PU:AFISV");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertTrue(d.isAbstract());
    assertTrue(d.isFinal());
    assertTrue(d.isImplicit());
    assertTrue(d.isStatic());
    assertTrue(d.isVolatile());

    d = new AdHocDecl("@CL:PU:ISvAF");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertTrue(d.isAbstract());
    assertTrue(d.isFinal());
    assertTrue(d.isImplicit());
    assertTrue(d.isStatic());
    assertTrue(d.isVolatile());

    d = new AdHocDecl("@CL:PU:a");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertTrue(d.isAbstract());
    assertFalse(d.isFinal());
    assertFalse(d.isImplicit());
    assertFalse(d.isStatic());
    assertFalse(d.isVolatile());

    d = new AdHocDecl("@CL:PU:f");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertTrue(d.isFinal());
    assertFalse(d.isImplicit());
    assertFalse(d.isStatic());
    assertFalse(d.isVolatile());

    d = new AdHocDecl("@CL:PU:i");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertFalse(d.isFinal());
    assertTrue(d.isImplicit());
    assertFalse(d.isStatic());
    assertFalse(d.isVolatile());

    d = new AdHocDecl("@CL:PU:s");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertFalse(d.isFinal());
    assertFalse(d.isImplicit());
    assertTrue(d.isStatic());
    assertFalse(d.isVolatile());

    d = new AdHocDecl("@CL:PU:v");
    assertSame(IDecl.Kind.CLASS, d.getKind());
    assertSame(IDecl.Visibility.PUBLIC, d.getVisibility());
    assertFalse(d.isAbstract());
    assertFalse(d.isFinal());
    assertFalse(d.isImplicit());
    assertFalse(d.isStatic());
    assertTrue(d.isVolatile());
  }
}
