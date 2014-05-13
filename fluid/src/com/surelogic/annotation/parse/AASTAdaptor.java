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
    return !FactoryRefs.registered.get(type);
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
          if (JavaNode.isSet(mods, JavaNode.PRIVATE) || JavaNode.isSet(mods, JavaNode.PROTECTED)) {
        	  throw new BadTokenException(tt, "Access modifier already set before 'public'");
          }
          mods = JavaNode.setModifier(mods, JavaNode.PUBLIC, true);
          return true;
        case SLAnnotationsParser.PROTECTED:
          if (JavaNode.isSet(mods, JavaNode.PRIVATE) || JavaNode.isSet(mods, JavaNode.PUBLIC)) {
        	  throw new BadTokenException(tt, "Access modifier already set before 'protected'");
          }
          mods = JavaNode.setModifier(mods, JavaNode.PROTECTED, true);
          return true;
        case SLAnnotationsParser.PRIVATE:
          if (JavaNode.isSet(mods, JavaNode.PUBLIC) || JavaNode.isSet(mods, JavaNode.PROTECTED)) {
        	  throw new BadTokenException(tt, "Access modifier already set before 'private'");
          }
          mods = JavaNode.setModifier(mods, JavaNode.PRIVATE, true);
          return true;
        case SLAnnotationsParser.STATIC:
          mods = JavaNode.setModifier(mods, JavaNode.STATIC, true);
          return true;
        case SLAnnotationsParser.FINAL:
          mods = JavaNode.setModifier(mods, JavaNode.FINAL, true);
          return true;
        case SLAnnotationsParser.VERIFY: 
          // Actually, setting the opposite, since it's true by default
          mods = JavaNode.setModifier(mods, JavaNode.NO_VERIFY, true);
          return true;
        case SLAnnotationsParser.IMPLEMENTATION_ONLY:
          mods = JavaNode.setModifier(mods, JavaNode.IMPLEMENTATION_ONLY, true);
          return true;
        case SLAnnotationsParser.ALLOW_REF_OBJECT:
            mods = JavaNode.setModifier(mods, JavaNode.ALLOW_REF_OBJECT, true);
            return true;          
      } 
      return false;
    }
  }
}
