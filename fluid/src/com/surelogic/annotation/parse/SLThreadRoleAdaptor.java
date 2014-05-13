/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/SLColorAdaptor.java,v 1.1 2007/10/22 18:26:10 dfsuther Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.Token;

import com.surelogic.parse.AbstractNodeAdaptor;
import com.surelogic.parse.TreeToken;

public class SLThreadRoleAdaptor extends AbstractNodeAdaptor {
  @Override
  protected Node newNode(String t, int type) {
    return new Node(t, type);
  }

  @Override
  protected boolean isRealToken(Token t) {
    int type = t.getType();
    return !ThreadRoleFactoryRefs.registered.get(type);
  }

  public class Node extends AbstractNodeAdaptor.Node {
    Node(String t, int type) {
      super(t, type);
    }
    
    @Override
    protected boolean handleSpecialTokens(TreeToken tt) {
// switch (tt.getType()) {
// case SLThreadRoleAnnotationsParser.RBRACKET:
// dims++;
// return false;
// case SLThreadRoleAnnotationsParser.PUBLIC:
// mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
// return true;
// case SLThreadRoleAnnotationsParser.PROTECTED:
// mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
// return true;
// case SLThreadRoleAnnotationsParser.PRIVATE:
// mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
// return true;
// case SLThreadRoleAnnotationsParser.STATIC:
// mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
// return true;
// case SLThreadRoleAnnotationsParser.FINAL:
// mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
// return true;
// }
      return false;
    }
  }
   
  
  
  
}

