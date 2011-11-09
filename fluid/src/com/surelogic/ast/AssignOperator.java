/*$Header$*/
package com.surelogic.ast;

/**
 * Any of the following:
 *    += -= *= /= %= <<= >>= >>>= &= ^= |=
 *    
 * @author chance
 */
public enum AssignOperator {
  PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), MOD("%"), 
  LEFT_SHIFT("<<"), RIGHT_SHIFT(">>"), UNSIGNED_RIGHT_SHIFT(">>>"), 
  AND("&"), XOR("^"), OR("|"), CONCAT("+");
  
  public final String token;

  AssignOperator(String t) {
    token = t;
  }
  @Override
  public String toString() {
    return token;
  }
}
