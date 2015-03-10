/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/TreeToken.java,v 1.6 2008/09/09 21:17:20 chance Exp $*/
package com.surelogic.parse;

import java.util.List;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
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

  public TreeToken(Token t) {
	super(t);
  }
  
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

  @Override
  public void addChild(Tree t) {
    throw new UnsupportedOperationException("Can't add children to "+this.text+": "+t.getText()+" "+t.getType());
  }

  @Override
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

  @Override
  public Tree getChild(int i) {
    throw new IllegalArgumentException("No children here");
  }

  @Override
  public int getChildCount() {
    return 0;
  }

  @Override
  public int getTokenStartIndex() {
    return 0;
  }

  @Override
  public int getTokenStopIndex() {
    return 0;
  }

  @Override
  public boolean isNil() {
    return false;
  }

  @Override
  public void setTokenStartIndex(int index) {
    // does nothing
  }

  @Override
  public void setTokenStopIndex(int index) {
    // does nothing
  }

  @Override
  public String toStringTree() {
    return toString();
  }

  @Override
  public Object deleteChild(int i) {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  @Override
  public void freshenParentAndChildIndexes() {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  @Override
  public int getChildIndex() {
	  return childIndex;
  }

  @Override
  public Tree getParent() {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  @Override
  public void replaceChildren(int i, int j, Object obj) {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  @Override
  public void setChild(int i, Tree tree) {
	  // TODO Auto-generated method stub
	  throw new UnsupportedOperationException();
  }

  @Override
  public void setChildIndex(int i) {
	  childIndex = i;
  }

  @Override
  public void setParent(Tree tree) {
	  parent = tree;
  }

  @Override
  public Tree getAncestor(int arg0) {
	// TODO Auto-generated method stub
	throw new UnsupportedOperationException();
  }

  @Override
  public List<?> getAncestors() {
	// TODO Auto-generated method stub
	throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasAncestor(int arg0) {
	// TODO Auto-generated method stub
	throw new UnsupportedOperationException();
  }
}
