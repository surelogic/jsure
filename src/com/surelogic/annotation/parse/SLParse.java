/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLParse.java,v 1.26 2008/09/19 20:58:02 chance Exp $*/
package com.surelogic.annotation.parse;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;

import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.parse.ASTFactory;
import com.surelogic.parse.TreeToken;

public class SLParse {
  public static void main(String[] args) throws Exception {
    for(int i=SLAnnotationsParser.START_IMAGINARY+1; i<SLAnnotationsParser.END_IMAGINARY; i++) {
      final String token = SLAnnotationsParser.tokenNames[i];
      if (!ASTFactory.getInstance().handles(token)) {
        System.out.println("WARNING: No factory for "+token);
      }
    }  
    printAST(initParser("static DefaultRegion").region().tree);
    printAST(initParser("DefaultRegion").region().tree);
    
    printAST(initParser("  return  ").uniqueExpression().tree);
    printAST(initParser("    this  ").thisExpr().tree);
    printAST(initParser("    this  ").borrowedFunction().tree);
    
    printAST(initParser("    nothing  ").starts().tree);
    
    printAST(initParser("  Object  ").simpleNamedType().tree);
    //printAST(initParser("  Object  ").namedType().tree);
    printAST(initParser(" java.lang.Object  ").namedType().tree);
    
    printAST(initParser(" Instance  ").regionName().tree);
    printAST(initParser(" [ ]  ").regionSpecification().tree);
    printAST(initParser(" Instance  ").regionSpecification().tree);
    
    printAST(initParser(" java.lang.Object:Instance  ").regionSpecification().tree);
    printAST(initParser(" Object:Instance  ").regionSpecification().tree);
    
    printAST(initParser(" L is this protects Instance  ").lock().tree);
    printAST(initParser("  Foo").lockName().tree);
    printAST(initParser("  Foo:Bar  ").lockName().tree);
    printAST(initParser("  Foo:Bar  ").requiresLock().tree);
    printAST(initParser("  Foo:Bar, Baz  ").requiresLock().tree);
    printAST(initParser("  Foo, Baz  ").requiresLock().tree);
    printAST(initParser("  Baz  ").isLock().tree);
    printAST(initParser("   Lock is this  ").policyLock().tree);
    printAST(initParser("  Yo  ").returnsLock().tree);    
    //printAST(initParser("  Foo.Bar:Baz  ").returnsLock().tree);    
    
    printAST(initParser("  [] into Instance  ").aggregate().tree); 
    printAST(initParser(" public static Foo   ").region().tree); 
    printAST(initParser(" private Bar extends Foo ").region().tree); 
    printAST(initParser("  bar:Instance  ").inRegion().tree); 
    printAST(initParser("  foo, Bar into Instance  ").mapFields().tree); 
    printAST(initParser("  [] into Instance  ").mapRegion().tree); 
    
    printAST(initParser("   Lock is last  ").policyLock().tree);
    
    //printAST(initParser("test.C.class  ").qualifiedClassLockExpression().tree);
    //printAST(initParser(" L3 is test.C.class protects i  ").lock().tree);
    
    //printAST(initParser("  Foo.Bar:Baz.readLock()  ").returnsLock().tree);  
    printAST(initParser("  is bogus ").testResult().tree, false);
    printAST(initParser("  is unassociated : blah ").testResult().tree, false);
    printAST(initParser(" /* is bogus ").testResultComment().tree, false);
    printAST(initParser(" /* is unassociated : blah ").testResultComment().tree, false);
    printAST(initParser("/**  is bogus ").testResultComment().tree, false);
    printAST(initParser(" /** is unassociated : blah ").testResultComment().tree, false);
    printAST(initParser("  is bogus & something_else ").testResult().tree, false);
    printAST(initParser("  is unassociated & tired : foo bar baz ").testResult().tree, false);
    
    printAST(initParser(" p:L  ").requiresLock().tree);
    //printAST(initParser("  no.such.packge.NoSuchClass:DoesntExist  ").returnsLock().tree); 
    
    printAST(initParser(" ").requiresLock().tree, true);
    printAST(initParser("Lock is Outer.this.lock protects Region1").lock().tree, true);
    printAST(initParser(" writes Region1; reads Region2 ").regionEffects().tree, true);
    printAST(initParser(" reads Region1, Region2, Region3; writes Region2, Region3").regionEffects().tree, true);
    printAST(initParser(" reads Region1; writes Region2, Region3").regionEffects().tree, true);
    printAST(initParser(" reads Region1, Region2, Region3; writes Region2").regionEffects().tree, true);
    printAST(initParser(" writes Region1, Region2; reads Region1, Region2, Region3 ").regionEffects().tree, true);
    printAST(initParser(" none ").regionEffects().tree, true);
  }

  public static SLAnnotationsParser initParser(String text) throws Exception { 
    @SuppressWarnings("deprecation")
    InputStream is = new StringBufferInputStream(text);
    
    // create a CharStream that reads from the stream above
    ANTLRInputStream input = new ANTLRInputStream(is);

    // create a lexer that feeds off of input CharStream
    SLAnnotationsLexer lexer = new SLAnnotationsLexer(input);

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    SLAnnotationsParser parser = new SLAnnotationsParser(tokens);

    AASTAdaptor adaptor = new AASTAdaptor();

    parser.setTreeAdaptor(adaptor);
    return parser;
  }

  public static void printAST(Object node) {
    printAST(node, true);
  }
  
  public static void printAST(Object node, boolean asAST) {
    if (node == null) {
      System.out.println("Null node");
      return;
    }
    if (node instanceof TreeToken) {
      TreeToken t = (TreeToken) node;
      System.out.println("token = "+t.getText());
      return;
    }

    AASTAdaptor.Node root = (AASTAdaptor.Node) node;
    System.out.println(root.toStringTree()); 
    if (asAST) {
      System.out.println(root.finalizeAST(IAnnotationParsingContext.nullPrototype).unparse(true));
    } else {
      System.out.println(root.finalizeId());
    }
  }
}

