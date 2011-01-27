/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/TestAdaptor.java,v 1.3 2008/09/09 21:17:20 chance Exp $*/
package com.surelogic.parse;

import org.antlr.runtime.Token;

import edu.cmu.cs.fluid.java.JavaNode;

public class TestAdaptor extends AbstractNodeAdaptor {
  @Override
  protected Node newNode(String t, int type) {
    return new Node(t, type);
  }
  
  @Override
  protected boolean isRealToken(Token t) {
    return t.getType() < JavaPrimitives.Tokens;
  }

  class Node extends AbstractNodeAdaptor.Node {
    Node(String t, int type) {
      super(t, type);
    } 
    @Override
    protected boolean handleSpecialTokens(TreeToken tt) {
      switch (tt.getType()) {
        case JavaPrimitives.RBRACKET:
          dims++;    
          return true;
        case JavaPrimitives.PUBLIC:
          mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
          return true;
        case JavaPrimitives.PROTECTED:
          mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
          return true;
        case JavaPrimitives.PRIVATE:
          mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
          return true;
        case JavaPrimitives.STATIC:
          mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
          return true;
        case JavaPrimitives.FINAL:
          mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
          return true;
      } 
      return false;
    }
  }
}
