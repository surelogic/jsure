package edu.cmu.cs.fluid.java;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.unparse.Keyword;
import edu.cmu.cs.fluid.unparse.SimpleTokenStream;
import edu.cmu.cs.fluid.unparse.Token;

public class DebugUnparser extends SimpleTokenStream implements JavaUnparser {
  private static final Logger LOG = SLLogger.getLogger("FLUID.java");
  protected static final Token deepToken = new Keyword("#");
  private static final Token nullToken = new Keyword("@NULL@");
  private final int maxLevel;
  private final SyntaxTreeInterface tree;

  DebugUnparser() {
    super();
    maxLevel = -1;
    tree = JJNode.tree;
  }

  DebugUnparser(int max) { 
    super(deepToken,max);
    maxLevel = max;
    tree = JJNode.tree;
  }

  public DebugUnparser(int max, SyntaxTreeInterface tree) {
    super(deepToken,max);
    maxLevel = max;
    this.tree = tree;
  }

  private int currLevel = 0;

  @Override
  public void unparse(IRNode node) {
    if (node == null) {
      nullToken.emit(this,node);
    } else if (currLevel == maxLevel) { // override if we've gone deep enough
      deepToken.emit(this,node);
    } else {
      // reimplements what SimpleFormatStream does, but more efficient,
      // because we can avoid unparsing the node altogether.
      try {
      	++currLevel;
	      JavaNode.unparse(node,this);
      } finally {
	      --currLevel;
      }
    }
  }

  @Override
  public JavaUnparseStyle getStyle() {
    return JavaUnparseStyle.prototype;
  }

  private static final int MAX = 5;
  private static final DebugUnparser[] reusableUnparsers = new DebugUnparser[MAX+2];
  static {
    for(int i=0; i<MAX+1; i++) {
        reusableUnparsers[i] = new DebugUnparser(i);
    }
    reusableUnparsers[MAX+1] = new DebugUnparser();
  }

  //private static final DebugUnparser reusableUnparser = reusableUnparsers[MAX];

  private static DebugUnparser getUnparser(int max) {
    if (max <= MAX) {
	    if (max < 0) { max = MAX+1; }
      reusableUnparsers[max].resetStream();
			reusableUnparsers[max].currLevel = 0;
	    return reusableUnparsers[max];
    } else {
	    return new DebugUnparser(max);
    }
  }

  private static final ThreadLocal<DebugUnparser> localUnparser = 
	  new ThreadLocal<DebugUnparser>() {
	  @Override
    protected DebugUnparser initialValue() {
		  return new DebugUnparser(MAX);
	  }
  };
  
  public static String toString(IRNode node) {
    return localUnparser.get().unparseString(node);
  }

  public static synchronized String toString(IRNode node, int max) {
    try {
      DebugUnparser du = getUnparser(max);
      du.unparse(node);
      return du.toString();
    } catch (RuntimeException e) {
      e.printStackTrace(System.err);
      LOG.log(Level.WARNING, "debugging toString failed", e);
      return "#<FAILED unparse of " + node + ">";
    }
  }

  public static String childrenToString(IRNode node) {
	  StringBuilder sb = new StringBuilder();
	  for(IRNode c : JJNode.tree.children(node)) {
		  sb.append('\t').append(toString(c)).append('\n');
	  }
	  return sb.toString();
  }
  
  public String unparseString(IRNode node) {
    resetStream();
    currLevel = 0;
    unparse(node);
    return toString();
  }
  
  @Override
  public SyntaxTreeInterface getTree() { return tree; }

  @Override
  public boolean isImplicit(IRNode node) {
    // TODO is this the right choice?  I think so.
    return false;
  }
  
  public static final IRNodeViewer viewer = new IRNodeViewer() {
    @Override
    public String toString(IRNode n) {
      return DebugUnparser.toString(n);
    }    
  };
  
  private static final JavaUnparseStyle noJavadocStyle = new JavaUnparseStyle(true, false);
  private static final JavaUnparseStyle noPromisesStyle = new JavaUnparseStyle(false, false);
  
  private static final DebugUnparser codeAndPromisesUnparser = new DebugUnparser(5, JJNode.tree) {
	  @Override
	  public JavaUnparseStyle getStyle() {
		  return noJavadocStyle;
	  }
  };
  
  public static String unparseCodeAndPromises(IRNode n) {
	  return codeAndPromisesUnparser.unparseString(n);
  }
  
  private static final DebugUnparser onlyCodeUnparser = new DebugUnparser(5, JJNode.tree) {
	  @Override
	  public JavaUnparseStyle getStyle() {
		  return noPromisesStyle;
	  }
  };
  
  public static String unparseCode(IRNode n) {
	  return onlyCodeUnparser.unparseString(n);
  }
}
