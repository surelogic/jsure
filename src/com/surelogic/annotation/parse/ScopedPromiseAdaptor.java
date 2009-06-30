/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/ScopedPromiseAdaptor.java,v 1.1 2007/08/24 18:22:02 chance Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.Token;

import com.surelogic.parse.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class ScopedPromiseAdaptor extends AbstractNodeAdaptor {
  @Override
  protected Node newNode(String t, int type) {
    return new Node(t, type);
  }
  
  @Override
  protected boolean isRealToken(Token t) { 
    int type = t.getType();
    return type < ScopedPromisesParser.START_IMAGINARY ||
           type > ScopedPromisesParser.END_IMAGINARY;
  }

  public class Node extends AbstractNodeAdaptor.Node {
    Node(String t, int type) {
      super(t, type);
    } 
    @Override
    protected boolean handleSpecialTokens(TreeToken tt) {
      switch (tt.getType()) {
        case ScopedPromisesParser.RBRACKET:
          dims++;    
          return false;
        case ScopedPromisesParser.PUBLIC:
          mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
          return true;
        case ScopedPromisesParser.PROTECTED:
          mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
          return true;
        case ScopedPromisesParser.PRIVATE:
          mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
          return true;
        case ScopedPromisesParser.STATIC:
          mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
          return true;
        case ScopedPromisesParser.FINAL:
          mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
          return true;
      } 
      return false;
    }
  }
}
