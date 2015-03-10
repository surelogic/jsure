package edu.cmu.cs.fluid.unparse;


import edu.cmu.cs.fluid.ir.IRNode;

public class OpenClose extends Token {
  boolean open;
  public OpenClose(boolean open) {
    this.open = open;
  }
  @Override
  public int getLength() {
    return 0;
  }
  public boolean getOpen() {
    return open;
  }
  @Override
  public void emit(TokenStream ts, IRNode aloc) {
    ts.append(open, this, aloc);
  }
  @Override
  public String toString () {
    if (open) return "<";
    return ">";
  }
	      
}
