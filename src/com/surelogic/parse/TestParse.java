/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/TestParse.java,v 1.15 2008/09/09 21:17:20 chance Exp $*/
package com.surelogic.parse;

import java.io.*;

import org.antlr.runtime.*;

import com.surelogic.annotation.IAnnotationParsingContext;

public class TestParse {
  public static void main(String[] args) throws Exception {
    for(int i=JavaPrimitives.Tokens+1; i<JavaPrimitives.tokenNames.length; i++) {
      final String token = JavaPrimitives.tokenNames[i];
      if (!ASTFactory.getInstance().handles(token)) {
        System.out.println("WARNING: No factory for "+token);
      }
    }   
    
    printAST(initParser("     public static final java.util:Map.Entry.Bar[][][] foo   ").fieldDecl().tree);
    printAST(initParser("    this  ").borrowed().tree);
    printAST(initParser("  return  ").unique().tree);
  }

  public static JavaPrimitives initParser(String text) throws Exception {
    @SuppressWarnings("deprecation")
    InputStream is = new StringBufferInputStream(text);
    
    // create a CharStream that reads from the stream above
    ANTLRInputStream input = new ANTLRInputStream(is);

    // create a lexer that feeds off of input CharStream
    JavaToken lexer = new JavaToken(input);

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    JavaPrimitives parser = new JavaPrimitives(tokens);

    TestAdaptor adaptor = new TestAdaptor();

    parser.setTreeAdaptor(adaptor);
    return parser;
  }

  public static void printAST(Object node) {
    if (node instanceof TreeToken) {
      TreeToken t = (TreeToken) node;
      System.out.println("token = "+t.getText());
      return;
    }
    TestAdaptor.Node root = (TestAdaptor.Node) node;
    System.out.println(root.toStringTree()); 
    System.out.println(root.finalizeAST(IAnnotationParsingContext.nullPrototype).unparse(true));
  }
}

