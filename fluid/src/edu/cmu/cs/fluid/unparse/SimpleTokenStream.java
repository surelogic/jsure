/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/unparse/SimpleTokenStream.java,v 1.4 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.unparse;

import edu.cmu.cs.fluid.ir.IRNode;

/** A token stream that simply builds up a string without any breaks.
 * A simple technique omits sections that are too deep.
 * @see FmtStream
 */
public class SimpleTokenStream implements TokenStream {
  private final int maxDepth;
  private StringBuilder sb = new StringBuilder();
  private final Token eToken;

  /** Create a token stream that puts all tokens on one line
   * and elides if we try to go deeper than maximum depth.
   * @param elisionToken use this token when eliding.
   */
  public SimpleTokenStream(Token elisionToken, int maximumDepth) {
    eToken = elisionToken;
    maxDepth = maximumDepth;
  }
  
  /** Create a token stream that puts all tokens on one
   * line and does not elide anything.
   */
  public SimpleTokenStream() {
    maxDepth = -1;
    eToken = null;
  }
  
  private boolean lastNotBP = false;
  private int depth = 0;
  private boolean eliding = true;

  public void resetStream() {
    lastNotBP = false;
    depth = 0;
    eliding = false;
    sb.setLength(0);
  }

  @Override
  public void append(Breakpoint bp, IRNode aloc) {
    if (!eliding) {  
      lastNotBP = false;
      int len = bp.getLength();
      if (len > 0) sb.append(' ');
    }
  }

  @Override
  public void append(Token tok, IRNode aloc) {
    if (!eliding) {
      if (lastNotBP)
	this.append(IndepBP.DEFAULTBP, aloc);
      lastNotBP = true;
      sb.append(tok.toString());
    }
  }

  @Override
  public void append(boolean open, Token tok, IRNode aloc) {
    if (!eliding && open && lastNotBP)
      this.append(IndepBP.DEFAULTBP, aloc);
    if (open) {
      if (depth++ == maxDepth) {
	append(eToken,aloc);
	eliding = true;
      }
    } else {
      if (--depth == maxDepth) {
	eliding = false;
      }
    }
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
