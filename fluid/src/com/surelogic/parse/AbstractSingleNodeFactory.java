/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/AbstractSingleNodeFactory.java,v 1.3 2007/06/04 19:02:41 chance Exp $*/
package com.surelogic.parse;

public abstract class AbstractSingleNodeFactory<T> extends AbstractASTFactory<T> {
  final String token;
  
  public AbstractSingleNodeFactory(String t) {
    token = t;
  }
  
  @Override
  public boolean handles(String token) {
    return this.token.equals(token);
  }
  
  @Override
  protected void checkToken(String token) {
    if (!handles(token)) {
      throw new IllegalArgumentException("Bad token type: "+token);
    }
  }
  
  @Override
  public IASTFactory<T> registerFactory(String token, IASTFactory<T> f) {
    throw new UnsupportedOperationException("Only deals with "+this.token);
  }
  
  public void register(IASTFactory<T> f) {
    f.registerFactory(token, this);
  }
}
