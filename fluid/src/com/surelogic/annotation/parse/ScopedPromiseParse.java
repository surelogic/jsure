/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/ScopedPromiseParse.java,v 1.27 2008/09/19 20:58:42 chance Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.*;

import com.surelogic.parse.*;

public class ScopedPromiseParse extends AbstractParse<ScopedPromisesParser> {
  public static final ScopedPromiseParse prototype = new ScopedPromiseParse();

  public static void main(String[] args) throws Exception {
	prototype.test();
  }

  private void test() throws Exception {
    for(int i=ScopedPromisesParser.START_IMAGINARY+1; i<ScopedPromisesParser.END_IMAGINARY; i++) {
      final String token = ScopedPromisesParser.tokenNames[i];
      if (!ASTFactory.getInstance().handles(token)) {
        System.out.println("WARNING: No factory for "+token);
      }
    }  
    printAST(initParser("public new(**)").promiseTarget().tree);
    printAST(initParser("public static *(**)").promiseTarget().tree);
    printAST(initParser("public !static *(**)").promiseTarget().tree);
    printAST(initParser("@ThreadRole(AWT) for fire*(**)").scopedPromise().tree);
    printAST(initParser("@ThreadRoleTransparent for revalidate()").scopedPromise().tree);
    printAST(initParser("@ThreadRoleTransparent for add*Listener(**) | remove*Listener(**)").scopedPromise().tree);    
    
    printAST(initParser("@Borrowed").scopedPromise().tree);
    printAST(initParser("@Borrowed()").scopedPromise().tree);
    printAST(initParser("@InRegion(TotalRegion)").scopedPromise().tree);
    printAST(initParser("int total").fieldDeclPattern().tree);
    printAST(initParser("int total").promiseTarget().tree);
    printAST(initParser("total*").promiseTarget().tree);
    printAST(initParser("@InRegion(TotalRegion) for int total*").scopedPromise().tree);
    
//    printAST(initParser("  Object*  ").wildcardIdentifier().tree, false);
//    printAST(initParser("  *Object  ").wildcardIdentifier().tree, false);
//    printAST(initParser("  *Object*  ").wildcardIdentifier().tree, false);
//    printAST(initParser("  java*  ").wildcardIdentifier().tree, false); 
//    
//    printAST(initParser("  java*Object  ").wildcardIdentifier().tree, false);
    
    printAST(initParser("  Object  ").name().tree);
    printAST(initParser(" java.Object.lang  ").name().tree, false);
    printAST(initParser("  Object  ").namedType().tree);
    printAST(initParser(" java.lang.Object  ").namedType().tree);

    System.out.println(initParser("@foo(foo)  ").scopedPromise().tree.getText());
    System.out.println(initParser("@foo(temp\n)  ").scopedPromise().tree.getText());
    System.out.println(initParser("@foo(a\n)").scopedPromise().tree.getText());
    System.out.println(initParser("@foo()").scopedPromise().tree.getText());
    // For some reason, these 1-char literals don't work.
    //System.out.println(initParser("'b'").scopedPromise().tree.getText());
    
    printAST(initParser("foobar*").wildcardIdentifier().tree, false);
    printAST(initParser("*bar").wildcardIdentifier().tree, false);
    
    printAST(initParser("  *bar").simpleNamePattern().tree);
    //printAST(initParser(" foo.").typeQualifierPattern().tree);
    printAST(initParser("* ").typeSigPattern().tree);
    printAST(initParser("* *bar in foo").fieldDeclPattern().tree);
    
    printAST(initParser("intValue(int) in foo").methodMatchPattern().tree, false);
    printAST(initParser("intValue() in foo").methodMatchPattern().tree, false);
    printAST(initParser("intValue()) in foo").noReturnMethodDeclPattern().tree, false);
    printAST(initParser("intValue(int)) in foo").baseTarget().tree, false);
    printAST(initParser("intValue(boolean)) in foo").andTarget().tree, false);
    printAST(initParser("intValue(**) & intValue(int) in foo").andTarget().tree, false);

    printAST(initParser("intValue()").methodMatchPattern().tree, false);
    printAST(initParser("intValue()").noReturnMethodDeclPattern().tree, false);
    printAST(initParser("intValue(int)").baseTarget().tree, false);
    printAST(initParser("intValue(boolean)").andTarget().tree, false);
    printAST(initParser("intValue(**) & intValue(int) in foo").andTarget().tree, false);
    
    //printAST(initParser(" foo*. ").typeQualifierPattern().tree, false);
    printAST(initParser(" * ").typeDeclPattern().tree, false);
    //printAST(initParser(" foo. ").typeQualifierPattern().tree, false);
    //printAST(initParser("  ").typeQualifierPattern().tree, false);
    
    printAST(initParser("*(**)").methodMatchPattern().tree, false);
    printAST(initParser("**(**)").methodMatchPattern().tree, false);
    
    printAST(initParser("*(**)").noReturnMethodDeclPattern().tree, false);
    printAST(initParser("*(**)").baseTarget().tree, false);
    printAST(initParser("*(**)").andTarget().tree, false);
    
    printAST(initParser("public **(**) & !(getSource(**))").andTarget().tree);
    printAST(initParser("new(**)").andTarget().tree);
    
    printAST(initParser("private public *(**)").andTarget().tree);    
    printAST(initParser("*(**)").promiseTarget().tree);
    printAST(initParser("intValue(**) in foo").promiseTarget().tree);
    printAST(initParser("intValue(**) in foo & intValue(int) in foo").promiseTarget().tree, false);
    printAST(initParser("intValue(**) in foo").promiseTarget().tree, false);
    
    //Methods
    printAST(initParser("@transparent").scopedPromise().tree);
    printAST(initParser("@transparent()").scopedPromise().tree);
    printAST(initParser("@reads(Foo)").scopedPromise().tree);
    printAST(initParser("@reads(nothing)").scopedPromise().tree);
    printAST(initParser("@reads(A)").scopedPromise().tree);
    printAST(initParser("@reads(B)").scopedPromise().tree);
    printAST(initParser("@reads(C)").scopedPromise().tree);
    printAST(initParser("@reads(D)").scopedPromise().tree);
    printAST(initParser("@reads(E)").scopedPromise().tree);
    printAST(initParser("@reads(F)").scopedPromise().tree);
    printAST(initParser("@reads(G)").scopedPromise().tree);
    printAST(initParser("@reads(H)").scopedPromise().tree);
    printAST(initParser("@reads(J)").scopedPromise().tree);
    printAST(initParser("@reads(K)").scopedPromise().tree);
    printAST(initParser("@reads(L)").scopedPromise().tree);
    printAST(initParser("@reads(M)").scopedPromise().tree);
    printAST(initParser("@reads(N)").scopedPromise().tree);
    printAST(initParser("@reads(O)").scopedPromise().tree);
    printAST(initParser("@reads(P)").scopedPromise().tree);
    printAST(initParser("@reads(Q)").scopedPromise().tree);
    printAST(initParser("@reads(R)").scopedPromise().tree);
    printAST(initParser("@reads(S)").scopedPromise().tree);
    printAST(initParser("@reads(T)").scopedPromise().tree);
    printAST(initParser("@reads(U)").scopedPromise().tree);
    printAST(initParser("@reads(V)").scopedPromise().tree);
    printAST(initParser("@reads(nstance)").scopedPromise().tree);
    printAST(initParser("@qwertyuiopasdfghjklzxcvbnm").scopedPromise().tree);
    printAST(initParser("@QWERTYUIOPASDFGHJKLZXCVBNM()").scopedPromise().tree);
    printAST(initParser("@reads(Instance)").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for intValue() in foo").scopedPromise().tree);
    printAST(initParser("@writes(nothing) for currentThread()").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for **(**)").scopedPromise().tree);
//    printAST(initParser("@reads(Instance for foo.intValue(long)").scopedPromise().tree);
    
    /* Omitting return types
    printAST(initParser("@reads(Instance for int foo.intValue()").scopedPromise().tree);
    printAST(initParser("@reads(Instance for int foo.intValue() & char foo.charValue()").scopedPromise().tree);
    printAST(initParser("@reads(Instance for int foo.intValue() & char foo.charValue() | foo.doubleValue()").scopedPromise().tree);
    //
    printAST(initParser("@reads(Instance for public int foo.intValue()").scopedPromise().tree);
    printAST(initParser("@reads(Instance for public int foo.intValue(int)").scopedPromise().tree);
    printAST(initParser("@reads(Instance for public int foo.intValue(int, boolean)").scopedPromise().tree);
    printAST(initParser("@reads(Instance for public int foo.intValue(int, boolean, String)").scopedPromise().tree);
    printAST(initParser("@reads(Instance for protected int foo.intValue()").scopedPromise().tree);
    printAST(initParser("@reads(Instance for protected static int foo.intValue()").scopedPromise().tree);
    printAST(initParser("@reads(Instance for protected !static int foo.intValue()").scopedPromise().tree);
    */
    
    printAST(initParser("@reads(Instance) for intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for intValue() in foo & charValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for intValue() in foo & charValue() in foo | doubleValue() in foo").scopedPromise().tree);
    //
    printAST(initParser("@reads(Instance) for public intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue(int) in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue(int, boolean) in foo").scopedPromise().tree);
    //printAST(initParser("@reads(Instance) for public intValue(int, boolean, String in foo)").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected static intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected !static intValue() in foo").scopedPromise().tree);
    
    //Using the 'in operator
    printAST(initParser("@reads(Instance) for intValue() in bar").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for intValue() in bar & charValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for intValue() in foo & charValue() in bar | doubleValue() in foobar").scopedPromise().tree);
    //
    printAST(initParser("@reads(Instance) for public intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue(int) in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue(int, boolean) in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue(int, boolean, String) in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected static intValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected !static intValue() in foo").scopedPromise().tree);
    
    printAST(initParser("@reads(Instance) for longValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public longValue() in foo").scopedPromise().tree);
    /* Omitting return types
    printAST(initParser("@reads(Instance) for int foo.longValue() throws Exception").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public int foo.longValue() throws Exception").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public int foo.longValue(int) throws Exception").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public int foo.longValue(int, long) throws Exception").scopedPromise().tree);
    */
    // Constructors
//    printAST(initParser("@reads(Instance) for Foo.new ()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for Foo.new (**)").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new ()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for private Foo.new ()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for Foo.new (int)").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for Foo.new (byte, int, char)").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for Foo.new (Foo, Foo)").scopedPromise().tree);
    // Constructors using 'in operator
    printAST(initParser("@reads(Instance) for new () in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for new (**) in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new () in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for private new () in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for new (int) in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for new (byte, int, char) in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for new (Foo, Foo) in Foo").scopedPromise().tree);
    
    // Fields
    printAST(initParser("  field").simpleNamePattern().tree);
//    printAST(initParser("* Foo.field").fieldDeclPattern().tree);
//    printAST(initParser("* Foo.field").baseTarget().tree);
//    printAST(initParser("@reads(Instance) for Foo Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for boolean Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public * Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for protected * Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for protected static * Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for static * Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for !static * Foo.field").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for * Foo.*").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for * Foo.field*").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for * Foo.*field").scopedPromise().tree);
    // Fields using 'in operator
    printAST(initParser("@reads(Instance) for Foo field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for boolean field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public * field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected * field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for protected static * field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for static * field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for !static * field in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for * * in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for * field* in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for * *field in Foo").scopedPromise().tree);
    
    
    //Types
    printAST(initParser("@reads(Instance) for Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for static Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for !static Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public static Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public !static Foo").scopedPromise().tree);
    //Types using 'in operator
    /*
    printAST(initParser("@reads(Instance) for Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for static Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for !static Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public static Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public !static Foo").scopedPromise().tree);
    */
    
    //Combinations
//    printAST(initParser("@reads(Instance) for public int foo.intValue() & public static char foo.charValue()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public int foo.intValue() & public char foo.charValue() | private foo.doubleValue()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public int foo.intValue() | public char foo.charValue() & private double foo.doubleValue()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public int foo.intValue() | public char foo.charValue() | private double foo.doubleValue()").scopedPromise().tree);
//    
//    printAST(initParser("@reads(Instance) for Foo.new() & public static char foo.charValue()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for Foo.new() | public static char foo.charValue()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new() & public char Foo.field | private Foo").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new() & public * Foo.field | private Foo | foo.longValue(**)").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new(**) & public char Foo.field | private Foo | foo.*()").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new(**) & public char Foo.field | private Foo | foo.*(**)").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new(**) & public char Foo.field | private Foo | foo.*(**) throws NewException").scopedPromise().tree);
//    printAST(initParser("@reads(Instance) for public Foo.new(**) & public char Foo.field | private Foo | foo.*(long)").scopedPromise().tree);
    //Combinations using the 'in operator
    printAST(initParser("@reads(Instance) for public intValue() in foo & public static charValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for (public intValue() & public static charValue()) in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue() in foo & public charValue() in foo | private doubleValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue() in foo | public charValue() in foo & private doubleValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public intValue() in foo | public charValue() in foo | private doubleValue() in foo").scopedPromise().tree);
    
    printAST(initParser("@reads(Instance) for new() in Foo & public static charValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for new() in Foo | public static charValue() in foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new() in Foo & public char field in Foo | private Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new() in Foo & public * field in Foo | private Foo | longValue(**) in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new(**) in Foo & public char field in Foo | private Foo | *() in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new(**) in Foo & public char field in Foo | private Foo | *(**) in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new(**) in Foo & public char field in Foo | private Foo | *(**) in Foo").scopedPromise().tree);
    printAST(initParser("@reads(Instance) for public new(**) in Foo & public char field in Foo | private Foo | *(long) in foo").scopedPromise().tree);

    printAST(initParser("mouse*(MouseEvent)").noReturnMethodDeclPattern().tree);
    printAST(initParser("java.awt.event.MouseEvent").paramTypeSigPattern().tree);
    printAST(initParser("mouse*(java.awt.event.MouseEvent)").noReturnMethodDeclPattern().tree);
    
    printAST(initParser("@reads(Instance) for * * in Bar").scopedPromise().tree); 
    printAST(initParser("@reads(Instance) for Bar").scopedPromise().tree); 
    
    printAST(initParser("*1").wildcardIdentifier().tree);
    printAST(initParser("private String *1 & !(private String *3)").andTarget().tree);
  }

  @Override
  protected TokenSource newLexer(CharStream input) {
	  return new ScopedPromisesLexer(input);
  }

  @Override
  protected ScopedPromisesParser newParser(TokenStream tokens) {
	  ScopedPromisesParser parser = new ScopedPromisesParser(tokens);

	  ScopedPromiseAdaptor adaptor = new ScopedPromiseAdaptor();

	  parser.setTreeAdaptor(adaptor);
	  return parser;
  }
}

