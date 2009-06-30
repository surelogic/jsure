/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorParse.java,v 1.4 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;


import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.parse.ASTFactory;
import com.surelogic.parse.TreeToken;

public class SLColorParse {
  public static void main(String[] args) throws Exception {
    for(int i=SLColorAnnotationsParser.START_IMAGINARY+1; i<SLColorAnnotationsParser.END_IMAGINARY; i++) {
      final String token = SLColorAnnotationsParser.tokenNames[i];
      if (!ASTFactory.getInstance().handles(token)) {
        System.out.println("WARNING: No factory for "+token);
      }
    }

    printAST(initParser(" foo ").name().tree, false);
    printAST(initParser("  foo, bar  ").colorDeclare().tree);
    
    printAST(initParser("foo").colorExpr().tree);
    
    printAST(initParser("(a&b) | (foo & !bar)").colorConstraint().tree);
    printAST(initParser("a").colorConstraint().tree);
    printAST(initParser("a.b.c | fooo.baar").colorConstraint().tree);
    printAST(initParser("!a").colorConstraint().tree);

    printAST(initParser("ren for AWT | NotYetVisible").colorRename().tree);
    
    printAST(initParser("a.b.c").colorImport().tree);
    
    printAST(initParser("(a & b) | c for Instance").colorConstrainedRegions().tree);
    
    printAST(initParser("a, b, c").colorGrant().tree);
    printAST(initParser("a, b, c").colorRevoke().tree);
    printAST(initParser("a, b, c").colorIncompatible().tree);
    
    printAST(initParser("Instance").colorized().tree);
    
    printAST(initParser(" ").colorTransparent().tree);

  }

  public static SLColorAnnotationsParser initParser(String text) throws Exception { 
    @SuppressWarnings("deprecation")
    InputStream is = new StringBufferInputStream(text);
    
    // create a CharStream that reads from the stream above
    ANTLRInputStream input = new ANTLRInputStream(is);

    // create a lexer that feeds off of input CharStream
    SLColorAnnotationsLexer lexer = new SLColorAnnotationsLexer(input);

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    SLColorAnnotationsParser parser = new SLColorAnnotationsParser(tokens);

    SLColorAdaptor adaptor = new SLColorAdaptor();

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

    SLColorAdaptor.Node root = (SLColorAdaptor.Node) node;
    System.out.println(root.toStringTree()); 
    if (asAST) {
      System.out.println(root.finalizeAST(IAnnotationParsingContext.nullPrototype).unparse(true));
    } else {
      System.out.println(root.finalizeId());
    }
  }

}
