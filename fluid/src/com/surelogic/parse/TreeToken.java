/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/TreeToken.java,v 1.6 2008/09/09 21:17:20 chance Exp $*/
package com.surelogic.parse;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.tree.Tree;

/**
 * A CommonToken that is also a leaf Tree node
 * 
 * @author Edwin.Chan
 */
public class TreeToken extends CommonToken implements Tree, Cloneable {
  private static final long serialVersionUID = 1L;
	
  int childIndex;
  Tree parent;
	
  public TreeToken(int type, String text) {
    super(type, convertText(type, text));
    start = Integer.MAX_VALUE;
    stop  = Integer.MIN_VALUE;
  }

  public TreeToken(CharStream input, int type, int channel, int start, int stop) {
    super(input, type, channel, start, stop);
  }
  
  private static String convertText(int type, String text) {
    if (text.equals("DOT")) {
      return ".";
    }
    if (text.equals("DSTAR")) {
      return "**";
    }
    if (text.equals("STAR")) {
      return "*";
    }
    return text;
  }

  public void addChild(Tree t) {
    throw new UnsupportedOperationException("Can't add children to "+this.text);
  }

  public Tree dupNode() {
    try {
      return (Tree) clone();
    } catch (CloneNotSupportedException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  public Tree dupTree() {
    return dupNode();
  }

  public Tree getChild(int i) {
    throw new IllegalArgumentException("No children here");
  }

  public int getChildCount() {
    return 0;
  }

  public int getTokenStartIndex() {
    return 0;
  }

  public int getTokenStopIndex() {
    return 0;
  }

  public boolean isNil() {
    return false;
  }

  public void setTokenStartIndex(int index) {
    // does nothing
  }

  public void setTokenStopIndex(int index) {
    // does nothing
  }

  public String toStringTree() {
    return toString();
  }

  public Object deleteChild(int i) {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  public void freshenParentAndChildIndexes() {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  public int getChildIndex() {
	  return childIndex;
  }

  public Tree getParent() {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  public void replaceChildren(int i, int j, Object obj) {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  public void setChild(int i, Tree tree) {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  public void setChildIndex(int i) {
	  childIndex = i;
  }

  public void setParent(Tree tree) {
	  parent = tree;
  }
}
