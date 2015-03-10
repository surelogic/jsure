package edu.cmu.cs.fluid.unparse;

import java.util.Vector;

class Render {
  TokenView tv;
  Vector<String> lines = new Vector<String>();

  public Render (TokenView tv) {
    this.tv = tv;
  }
}
      
