/*$Header$*/
package com.surelogic.tree;

import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.tree.*;

class Constants {
  static final IRNode undefinedNode = new MarkedIRNode("undefined");
  static final IRSequence<IRNode> undefinedSequence = new Simple1IRArray<IRNode>();
  static final IRLocation undefinedLocation = IRLocation.getSentinel();
  static final Operator undefinedOperator = new Operator() {
    @Override
    public String name() {
      return "undefined";
    }
    @Override
    public SyntaxTreeInterface tree() {
      throw new NotImplemented();
    }
    @Override
    public IRNode createNode() {
      throw new NotImplemented();
    }
    @Override
    public IRNode createNode(SyntaxTreeInterface tree) {
      throw new NotImplemented();
    }
  };
  static final IJavaRef undefinedSrcRef = null;
  static final String undefinedString = new String();
  static final Integer undefinedInteger = new Integer(JavaNode.ILLEGAL_MOD);
}
