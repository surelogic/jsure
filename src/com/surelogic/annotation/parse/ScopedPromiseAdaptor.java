/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/ScopedPromiseAdaptor.java,v 1.1 2007/08/24 18:22:02 chance Exp $*/
package com.surelogic.annotation.parse;

import org.antlr.runtime.Token;

import com.surelogic.annotation.IAnnotationParsingContext;
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
	private int textStart = Integer.MAX_VALUE;
	private int textStop = Integer.MIN_VALUE;
	  
    Node(String t, int type) {
      super(t, type);
    } 
    @Override
    protected boolean handleSpecialTokens(TreeToken tt) {
      // Hack to collect data for ScopedPromise
      updateTextRange(tt.getStartIndex(), tt.getStopIndex());
      
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
    
    private void updateTextRange(int begin, int end) {
        if (begin < textStart) {
        	textStart = begin;
        }
        if (textStop < end) {
        	textStop = end;
        }
    }

    /**
     * Use the original text (from the context), based on the start/stop offsets
     * (not including child nodes)
     * 
     * Really here only for ScopedPromiseNode
     */
	public void useText(IAnnotationParsingContext c) {
		final String text = c.getSelectedText(textStart, textStop+1);
		id = text;
	}
  }
}
