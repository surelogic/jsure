/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorParse.java,v 1.4 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.*;

import com.surelogic.parse.*;

public class SLColorParse extends AbstractParse<SLColorAnnotationsParser> {
  public static final SLColorParse prototype = new SLColorParse();

  public static void main(String[] args) throws Exception {
	prototype.test();
  }
	  
  private void test() throws Exception {
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

  @Override
  protected TokenSource newLexer(CharStream input) {
	  return new SLColorAnnotationsLexer(input);
  }

  @Override
  protected SLColorAnnotationsParser newParser(TokenStream tokens) {
	  SLColorAnnotationsParser parser = new SLColorAnnotationsParser(tokens);

	  SLColorAdaptor adaptor = new SLColorAdaptor();

	  parser.setTreeAdaptor(adaptor);
	  return parser;
  }
}
