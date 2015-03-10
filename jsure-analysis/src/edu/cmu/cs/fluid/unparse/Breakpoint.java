package edu.cmu.cs.fluid.unparse;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Abstract class for spots in the format stream where
 * the code can break
 * 
 * @author chance
 *
 */
abstract public class Breakpoint extends Token {
  Breakpoint() { // FIX get rid of this
  }
  Breakpoint(int priority, Glue horz, Glue indent) {
    prio = priority;
    horzGlue = horz;
    indentGlue = indent;
  }
 
  int prio;       // lower numbers break first
  Glue horzGlue;   // glue if no break
  Glue indentGlue; // glue increment if break
  // Notes on Breakpoints:
  //		- Productions never end or start with breakpoints!
  //		- Indent adds to previous level/prio.
  static int MAXPRIO = 99;
  public int getPrio () {
    return prio;
  }
  public String getName () {
    return "";
  }
  @Override
  public void emit(TokenStream ts, IRNode aloc) {
    ts.append(this, aloc);
  }
  @Override
  public int getLength() {
    return horzGlue.getLength();
  }
  @Override
  public String toString() {
    return ".";
  }
}
