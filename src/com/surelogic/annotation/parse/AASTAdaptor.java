/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/AASTAdaptor.java,v 1.5 2007/08/24 18:17:32 chance Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.Token;

import com.surelogic.parse.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class AASTAdaptor extends AbstractNodeAdaptor {
  @Override
  protected Node newNode(String t, int type) {
    return new Node(t, type);
  }
  
  @Override
  protected boolean isRealToken(Token t) { 
    int type = t.getType();
    return type < SLAnnotationsParser.START_IMAGINARY ||
           type > SLAnnotationsParser.END_IMAGINARY;
  }

  public class Node extends AbstractNodeAdaptor.Node {
    Node(String t, int type) {
      super(t, type);
    } 
    @Override
    protected boolean handleSpecialTokens(TreeToken tt) {
      switch (tt.getType()) {
        case SLAnnotationsParser.RBRACKET:
          dims++;    
          return false;
        case SLAnnotationsParser.PUBLIC:
          mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
          return true;
        case SLAnnotationsParser.PROTECTED:
          mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
          return true;
        case SLAnnotationsParser.PRIVATE:
          mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
          return true;
        case SLAnnotationsParser.STATIC:
          mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
          return true;
        case SLAnnotationsParser.FINAL:
          mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
          return true;
      } 
      return false;
    }
  }
}
