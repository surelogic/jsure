package edu.cmu.cs.fluid.unparse;

import java.util.Stack;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class FmtStream implements TokenStream {
  TokenArray t;
  boolean lastNotBP;
  Stack<IRNode> astStack = new Stack<IRNode>();

  boolean debug;
  StringBuilder s; // For debug

  public FmtStream(boolean debug) {
    this.debug = debug;
    t = new TokenArray();
    resetStream();
  }

  public void resetStream() {
    t.init();
    if (debug) s = new StringBuilder("");
    prepStream();
  }

  public void prepStream() {
    lastNotBP = false;
  }

  @Override
  public String toString() {
    if (debug)
      return s.toString();
    else return "Not debugging.";
  }

  public TokenArray getTokenArray() {
    return t;
  }

  @Override
  public void append(Breakpoint bp, IRNode aloc) {
    lastNotBP = false;
    if (debug) s.append(".");
    t.insertToken(aloc, bp);
  }

  @Override
  public void append(Token tok, IRNode aloc) {
    if (lastNotBP)
      this.append(IndepBP.DEFAULTBP, aloc);
    lastNotBP = true;
    if (debug) s.append(tok.toString());
    t.insertToken(aloc, tok);
  }

  @Override
  public void append(boolean open, Token tok, IRNode aloc) {
    if (open && lastNotBP)
      this.append(IndepBP.DEFAULTBP, astStack.peek());
    if (open) astStack.push(aloc); else astStack.pop();
    if (debug) {if (open) s.append("["); else s.append("]");}
    t.insertToken(aloc, open, tok);
  }

  public abstract void unparse(IRNode node);
  
  /**
   * An implicit node is one which does not occur in the unparsing.
   * The unparsing syntax may specify that some of the other  tokens
   * are also omitted.
   * @param node node to check whether implicit
   * @return true if the ir node should not be unparsed.
   */
  public abstract boolean isImplicit(IRNode node);
}
