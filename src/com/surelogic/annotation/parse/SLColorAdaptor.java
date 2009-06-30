/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorAdaptor.java,v 1.1 2007/10/22 18:26:10 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.Token;

import com.surelogic.annotation.parse.AASTAdaptor.Node;
import com.surelogic.parse.AbstractNodeAdaptor;
import com.surelogic.parse.TreeToken;

import edu.cmu.cs.fluid.java.JavaNode;

public class SLColorAdaptor extends AbstractNodeAdaptor {
  @Override
  protected Node newNode(String t, int type) {
    return new Node(t, type);
  }

  @Override
  protected boolean isRealToken(Token t) {
    int type = t.getType();
    return type < SLColorAnnotationsParser.START_IMAGINARY
        || type > SLColorAnnotationsParser.END_IMAGINARY;
  }

  public class Node extends AbstractNodeAdaptor.Node {
    Node(String t, int type) {
      super(t, type);
    }
    
    @Override
    protected boolean handleSpecialTokens(TreeToken tt) {
// switch (tt.getType()) {
// case SLColorAnnotationsParser.RBRACKET:
// dims++;
// return false;
// case SLColorAnnotationsParser.PUBLIC:
// mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
// return true;
// case SLColorAnnotationsParser.PROTECTED:
// mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
// return true;
// case SLColorAnnotationsParser.PRIVATE:
// mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
// return true;
// case SLColorAnnotationsParser.STATIC:
// mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
// return true;
// case SLColorAnnotationsParser.FINAL:
// mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
// return true;
// }
      return false;
    }
  }
   
  
  
  
}

