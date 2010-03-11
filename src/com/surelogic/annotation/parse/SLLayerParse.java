/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorParse.java,v 1.4 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.*;

import com.surelogic.parse.*;

public class SLLayerParse extends AbstractParse<LayerPromisesParser> {
  public static final SLLayerParse prototype = new SLLayerParse();

  public static void main(String[] args) throws Exception {
	prototype.test();
  }
	  
  private void test() throws Exception {
    for(int i=LayerPromisesParser.START_IMAGINARY+1; i<LayerPromisesParser.END_IMAGINARY; i++) {
      final String token = LayerPromisesParser.tokenNames[i];
      if (!ASTFactory.getInstance().handles(token)) {
        System.out.println("WARNING: No factory for "+token);
      }
    }

    //printAST(initParser(" foo ").name().tree, false);
  }
  
  @Override
  protected TokenSource newLexer(CharStream input) {
	  return new LayerPromisesLexer(input);
  }

  @Override
  protected LayerPromisesParser newParser(TokenStream tokens) {
	  LayerPromisesParser parser = new LayerPromisesParser(tokens);

	  SLColorAdaptor adaptor = new SLColorAdaptor();

	  parser.setTreeAdaptor(adaptor);
	  return parser;
  }
}
