// Generated code.  Do *NOT* edit!
package edu.cmu.cs.fluid.parse;

// Generated from C:\work\workspace\fluid/ops/edu/cmu/cs/fluid\parse\Ellipsis.op:  Do *NOT* edit!

import java.util.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.unparse.Token;
import edu.cmu.cs.fluid.unparse.*;
import edu.cmu.cs.fluid.util.*;

@SuppressWarnings("all")
public class Ellipsis extends JavaOperator implements IAcceptor, ClassBodyDeclInterface, ForInitInterface, ImportNameInterface, OptArrayInitializerInterface, ReturnTypeInterface, StatementExpressionInterface, TypeDeclInterface, TypeInterface 
{
  protected Ellipsis() {}

  public static final Ellipsis prototype = new Ellipsis();

  @Override
  public boolean isProduction() {
    return true;
  }

  @Override
  public Operator childOperator(int i) {
    return JavaOperator.prototype;
  }

  @Override
  public Operator childOperator(IRLocation loc) {
    return JavaOperator.prototype;
  }

  @Override
  public String childLabel(int i) {
    return "child";
  }

  @Override
  public String infoType(int i) {
    return "";
  }

  @Override
  public String infoLabel(int i) {
    return "";
  }

  public Operator variableOperator() {
    return JavaOperator.prototype;
  }

  @Override
  public int numInfo() {
    return 0;
  }

  @Override
  public int numChildren() {
    return -1;
  }

  public static JavaNode createNode(IRNode[] child) {
    return createNode(tree, child);
  }
  public static JavaNode createNode(SyntaxTreeInterface tree, IRNode[] child) {
    JavaNode _result = JavaNode.makeJavaNode(tree, prototype,child);
    return _result;
  }

  public static IRNode getChild(IRNode node, int i) {
    return getChild(tree, node, i);
  }

  public static Iteratable<IRNode> getChildIterator(IRNode node) {
    return getChildIterator(tree, node);
  }

  public static IRNode getChild(SyntaxTreeInterface tree, IRNode node, int i) {
    Operator op = tree.getOperator(node);
    if (!(op instanceof Ellipsis)) {
      throw new IllegalArgumentException("node not Ellipsis: "+op);
    }
    return tree.getChild(node,0+i);
  }

  public static Iteratable<IRNode> getChildIterator(SyntaxTreeInterface tree, IRNode node) {
    Operator op = tree.getOperator(node);
    if (!(op instanceof Ellipsis)) {
      throw new IllegalArgumentException("node not Ellipsis: "+op);
    }
    Iteratable<IRNode> _result = tree.children(node);
    return _result;
  }

  private static Token littoken1 = new Delim("...");

  @Override
  public Token asToken() {
    return littoken1;
  }

  @Override public void unparse(IRNode node, JavaUnparser unparser) {
    SyntaxTreeInterface tree = unparser.getTree();
    Operator op = tree.getOperator(node);
    if (!(op instanceof Ellipsis)) {
      throw new IllegalArgumentException("node not Ellipsis: "+op);
    }
    Iteratable<IRNode> e = tree.children(node);
    littoken1.emit(unparser,node);
    while (e.hasNext()) {
      unparser.unparse(e.next());
    }
  }

  @Override
  public boolean isMissingTokens(IRNode node)  {
    return true;
  }

  @Override
  public Vector<Token>[] missingTokens(IRNode node) {
    SyntaxTreeInterface tree = JJNode.tree;
    Operator op = tree.getOperator(node);
    if (!(op instanceof Ellipsis)) {
      throw new IllegalArgumentException("node not Ellipsis: "+op);
    }
    Iteratable<IRNode> e = tree.children(node);
    int i = 0;
    int numChildren = tree.numChildren(node);
    Vector<Token>[] TokenList = new Vector[numChildren+1];
    for (int j = 0; j < numChildren + 1; j++)
       TokenList[j] = new Vector<Token>();
    TokenList[i].add(littoken1);
    while (e.hasNext()) {
      e.next();
      i++;
    }
    return TokenList;
  }

  public <T> T accept(IRNode node, IVisitor<T> visitor) {
    return visitor.visitEllipsis(node);
  }

    public String name() { return "..."; }
  
  public IRNode get_Body(IRNode n) {
    return null;
  }
  
  public Operator getResultOp() {
    return Ellipsis.prototype;
  }
  
  public void unparseWrapper(IRNode node, JavaUnparser u) {
    OPENTOKEN.emit(u, node);
    unparse(node, u);
    CLOSETOKEN.emit(u, node);
  }
}
