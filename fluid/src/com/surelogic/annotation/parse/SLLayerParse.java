/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorParse.java,v 1.4 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.*;

public class SLLayerParse extends AbstractParse<LayerPromisesParser> {
  public static final SLLayerParse prototype = new SLLayerParse();

  public static void main(String[] args) throws Exception {
	prototype.test();
  }
	  
  private void test() throws Exception {
    ParseUtil.init();
    printAST(initParser("java.util").mayReferTo().tree, true);
    printAST(initParser("edu.afit.smallworld.{model,persistence} | java.io.File").mayReferTo().tree, true);
    printAST(initParser("edu.afit.smallworld.model | org.jdom+ | java.{io,net,util}").mayReferTo().tree, true);
    printAST(initParser("UTIL = java.util & !(java.util.{Enumeration,Hashtable,Vector})").typeSet().tree, true);
    printAST(initParser("XML = org.jdom+ | UTIL  | java.{io,net}").typeSet().tree, true);
    printAST(initParser("UTIL").typeTarget().tree, true);
    printAST(initParser("MODEL may refer to UTIL").layer().tree, true);
    printAST(initParser("PERSISTENCE may refer to MODEL | XML").layer().tree, true);
    printAST(initParser("CONTROLLER may refer to MODEL | PERSISTENCE | java.io.File").layer().tree, true);
    printAST(initParser("edu.afit.smallworld.MODEL").inLayer().tree, true);
    printAST(initParser("edu.afit.smallworld.PERSISTENCE").inLayer().tree, true);
    printAST(initParser("edu.afit.smallworld.CONTROLLER").inLayer().tree, true);
  }
  
  @Override
  protected TokenSource newLexer(CharStream input) {
	  return new LayerPromisesLexer(input);
  }

  @Override
  protected LayerPromisesParser newParser(TokenStream tokens) {
	  LayerPromisesParser parser = new LayerPromisesParser(tokens);

	  SLLayerAdaptor adaptor = new SLLayerAdaptor();

	  parser.setTreeAdaptor(adaptor);
	  return parser;
  }
}
