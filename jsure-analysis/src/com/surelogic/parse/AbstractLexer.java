/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/AbstractLexer.java,v 1.2 2008/09/09 21:17:20 chance Exp $*/
package com.surelogic.parse;

import org.antlr.runtime.*;

public abstract class AbstractLexer extends Lexer {
  @Override
  public final Token emit() {
    Token t = makeToken(input, state.type, state.channel, state.tokenStartCharIndex, getCharIndex()-1);
    t.setLine(state.tokenStartLine);
    t.setText(state.text);
    t.setCharPositionInLine(state.tokenStartCharPositionInLine);
    emit(t);
    return t;
  }

  /**
   * Override this to create different tokens
   * @param input
   * @param type
   * @param channel
   * @param startIndex
   * @param stopIndex
   * @return
   */
  protected Token makeToken(CharStream input, int type, int channel, int startIndex, int stopIndex) {
    return new CommonToken(input, type, channel, startIndex, stopIndex);
  }
}
