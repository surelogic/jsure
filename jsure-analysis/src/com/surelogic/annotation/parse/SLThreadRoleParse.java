/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorParse.java,v 1.4 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.*;

public class SLThreadRoleParse extends AbstractParse<SLThreadRoleAnnotationsParser> {
  public static final SLThreadRoleParse prototype = new SLThreadRoleParse();

  public static void main(String[] args) throws Exception {
	prototype.test();
  }
	  
  private void test() throws Exception {
	ParseUtil.init();

    printAST(initParser(" foo ").name().tree, false);
    printAST(initParser("  foo, bar  ").threadRole().tree);
    
    printAST(initParser("foo").threadRoleExpr().tree);
    
    printAST(initParser("(a&b) | (foo & !bar)").threadRoleConstraint().tree);
    printAST(initParser("a").threadRoleConstraint().tree);
    printAST(initParser("a.b.c | fooo.baar").threadRoleConstraint().tree);
    printAST(initParser("!a").threadRoleConstraint().tree);

    printAST(initParser("ren for AWT | NotYetVisible").threadRoleRename().tree);
    
    printAST(initParser("a.b.c").threadRoleImport().tree);
    
    printAST(initParser("(a & b) | c for Instance").threadRoleConstrainedRegions().tree);
    
    printAST(initParser("a, b, c").threadRoleGrant().tree);
    printAST(initParser("a, b, c").threadRoleRevoke().tree);
    printAST(initParser("a, b, c").threadRoleIncompatible().tree);
    
    printAST(initParser("Instance").regionReportRoles().tree);
    
    printAST(initParser(" ").threadRoleTransparent().tree);
  }

  @Override
  protected TokenSource newLexer(CharStream input) {
	  return new SLThreadRoleAnnotationsLexer(input);
  }

  @Override
  protected SLThreadRoleAnnotationsParser newParser(TokenStream tokens) {
	  SLThreadRoleAnnotationsParser parser = new SLThreadRoleAnnotationsParser(tokens);

	  SLThreadRoleAdaptor adaptor = new SLThreadRoleAdaptor();

	  parser.setTreeAdaptor(adaptor);
	  return parser;
  }
}
