package edu.cmu.cs.fluid.unparse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;


public class TokenArray {
  // Token stream object (unparse -> format)
  Token[] tokens;
  IRNode[] tokAsts;
  SlotInfo<NodeLoc> nodeLocInfo;
  // Temporary slot to hold nodeLocs.

  static int TOKCAPACITY = 100000; //!! FIX: use vectors  
  int level;
  int top;

  TokenArray() {
    tokens = new Token[TOKCAPACITY];     
    // This lexical token, bkpt, or open/close.
    tokAsts = new IRNode[TOKCAPACITY];  
    // The IRNode that generated *this* token
  }

  public void init() {
    level = 0;
    top = 0;
    nodeLocInfo = SimpleSlotFactory.prototype.newAttribute();
    insertToken(null, MandatoryBP.BREAKBP);
  }

  public void finish() {
    insertToken(null, MandatoryBP.BREAKBP);
  }

  public void insertToken(IRNode aloc, Token tok) {
    tokens[top] = tok;
    tokAsts[top] = aloc;
    top++;
  }

  public void insertToken(IRNode aloc, boolean open, Token tok) {
    if (open) {
      aloc.setSlotValue(nodeLocInfo, new NodeLoc(top, 0));
    }
    else {
      (aloc.getSlotValue(nodeLocInfo)).end = top;
    }
    insertToken(aloc,tok);
  }

  public int getNodeOpen(IRNode aloc) {
    return (aloc.getSlotValue(nodeLocInfo)).start;
  }

  public int getNodeClose(IRNode aloc) {
    return (aloc.getSlotValue(nodeLocInfo)).end;
  }

  public Token getToken(int index) {
    return tokens[index];
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    for (int i=0; i < top; i++) {
      s.append(getToken(i).toString());
    }
    return s.toString();
  }
}

class NodeLoc {
  int start, end;
  // Locations of open and close tokens in the TokenArray (for an IRNode)

  NodeLoc(int start, int end) {
    this.start = start;
    this.end = end;
  }
}
