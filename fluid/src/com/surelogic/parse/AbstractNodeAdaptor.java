/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/AbstractNodeAdaptor.java,v 1.14 2008/10/01 20:52:40 chance Exp $*/
package com.surelogic.parse;

import java.util.*;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.*;

import com.surelogic.aast.AASTNode;
import com.surelogic.annotation.IAnnotationParsingContext;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.java.JavaNode;

public abstract class AbstractNodeAdaptor extends CommonTreeAdaptor {
  protected abstract Node newNode(String t, int type);
  
  @Override
  public Object create(Token t) {      
    if (t == null) {
      return newNode("null", -1);
    }
    if (isRealToken(t)) {
      return t;
    }
    return newNode(t.getText(), t.getType());
  }
  
  protected abstract boolean isRealToken(Token t);

  @Override
  public Token createToken(int tokenType, String text) {
    return new TreeToken(tokenType, text);
  }

  @Override
  public Object rulePostProcessing(final Object root) {
    if (root == null) {
      return null;
    }
    Object newRoot = super.rulePostProcessing(root);      
    if (newRoot == null) {
    	return root;
    	// System.out.println(root);
    }
    
    /* From BaseTree
    Node rv = (Node) root;
    if ( rv!=null && rv.isNil() && rv.getChildCount()==1 ) {
      root = rv.children.get(0);
    }
    */
    if (newRoot instanceof TreeToken) {
      return newRoot;
    } 
    Node rv = (Node) newRoot;
    rv.postProcessChildren();
    return rv;
  }
  
  public abstract class Node extends BaseTree {
    final String token;
    final int type;
    protected String id = "";
    protected int dims  = 0;
    protected int start = Integer.MAX_VALUE;
    protected int stop  = Integer.MIN_VALUE;
    protected int mods  = JavaNode.ALL_FALSE;
    
    protected Node(String t, int ty) {
//      if (!"null".equals(t)) {
//        System.out.println(t);
//      }
      token = t;
      type  = ty;
    }
    
    public int getModifiers() {
    	return mods;
    }
    
    @Override
    public boolean isNil() {
      return token == "null";
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void addChild(Tree t) {    
      if (t instanceof TreeToken) {
        if (this.children == null) {
          this.children = createChildrenList();
        }
        this.children.add(t);
      } else {
        super.addChild(t);
      }
    }
    
    void updateRange(int begin, int end) {
      if (begin < start) {
        start = begin;
      }
      if (stop < end) {
        stop = end;
      }
    }
    
    @SuppressWarnings("unchecked")
    void postProcessChildren() {
      // Handle any TreeTokens 
      if (!this.isNil() && this.children != null) {
        for(Object o : new ArrayList(this.children)) {
          if (o instanceof TreeToken) {
            this.handleToken((TreeToken) o);
            this.children.remove(o);
          } else {
            Node n = (Node) o;
            this.updateRange(n.start, n.stop);
          }
        }
      }
    }
    
    void handleToken(TreeToken tt) {
      if (isRealToken(tt)) {
        updateRange(tt.getStartIndex(), tt.getStopIndex());
        
        boolean handled = handleSpecialTokens(tt);
        if (handled) {
          return;
        }
        if (id.length() == 0) {
          id = tt.getText();          
        } else {
          id = id + tt.getText();
        }              
      } else {
//        System.out.println("Ignoring "+tt.getText());
      }
    }

    protected abstract boolean handleSpecialTokens(TreeToken tt);

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(token);
      if (start < Integer.MAX_VALUE) {
        sb.append(':').append(start);
      } else {
        sb.append(":?");
      }
      if (stop > Integer.MIN_VALUE) {
        sb.append(':').append(stop);
      } else {
        sb.append(":?");
      }
      if (JavaNode.getModifier(mods, JavaNode.PUBLIC)) {
        sb.append(" public");
      }
      if (JavaNode.getModifier(mods, JavaNode.PROTECTED)) {
        sb.append(" protected");
      }
      if (JavaNode.getModifier(mods, JavaNode.PRIVATE)) {
        sb.append(" private");
      }
      if (JavaNode.getModifier(mods, JavaNode.STATIC)) {
        sb.append(" static");
      }
      if (JavaNode.getModifier(mods, JavaNode.FINAL)) {
        sb.append(" final");
      }
      if (id.length() != 0) {
        sb.append(" id=").append(id);
      }
      if (dims != 0) {
        sb.append(" dims=").append(dims);
      }
      return sb.toString();
    }

    @Override
    public Tree dupNode() {
      throw new NotImplemented();
    }

    @Override
    public String getText() {
      return id;
    }

    @Override
    public int getType() {
      return type;
    }
    
    @Override
    public int getTokenStartIndex() {
      return start;
    }

    @Override
    public int getTokenStopIndex() {
      return stop;
    }

    @Override
    public void setTokenStartIndex(int index) {
    }

    @Override
    public void setTokenStopIndex(int index) {
    }
    
    public AASTNode finalizeAST(IAnnotationParsingContext context) {
      List<AASTNode> kids;
      postProcessChildren(); // FIX requires another pass throught children
      
      if (children != null) {
        kids = new ArrayList<AASTNode>();
        for(Object o : children) {
          Node n = (Node) o;
          kids.add(n.finalizeAST(context));
        }
      } else {
        kids = Collections.emptyList();
      }
      int start = context.mapToSource(this.start);
      int stop  = context.mapToSource(this.stop);
      return ASTFactory.getInstance().create(token, start, stop, mods | context.getModifiers(), id, dims, kids);
    }
    
    public String finalizeId() {
      postProcessChildren();
      return id;
    }
    
    public void setModifier(int flag) {
      boolean found = false;
      for(int mod : JavaNode.MODIFIERS) {
        if (mod == flag) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new IllegalArgumentException("Unknown flag: "+flag);
      }
      mods |= flag;
    }
  }
}
