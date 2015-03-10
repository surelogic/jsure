/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaFmtStream.java,v 1.7
 * 2003/07/03 00:21:56 chance Exp $
 */
package edu.cmu.cs.fluid.java;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.unparse.FmtStream;

public class JavaFmtStream extends FmtStream implements JavaUnparser {
  static final Logger LOG = SLLogger.getLogger("JAVA.unparse");

  private final JavaUnparseStyle style;
  private final SyntaxTreeInterface tree;

  public JavaFmtStream(boolean debug) {
    super(debug);
    style = new JavaUnparseStyle();
    tree = JJNode.tree;
  }

  public JavaFmtStream(boolean debug, SyntaxTreeInterface tree) {
    super(debug);
    style = new JavaUnparseStyle();
    this.tree = tree;
  }

  public JavaFmtStream(
    boolean debug,
    SyntaxTreeInterface tree,
    JavaUnparseStyle unparseStyle) {
    super(debug);
    style = unparseStyle;
    this.tree = tree;
  }

  @Override
  public void unparse(IRNode node) {
    try {
      if (!isImplicit(node)) JavaNode.unparse(node, this);
    } catch (SlotUndefinedException e) {
      LOG.log(Level.SEVERE, "Died on " + DebugUnparser.toString(node), e);
      /*
			 * if (tree instanceof SyntaxForestModel) { SyntaxForestModel model =
			 * (SyntaxForestModel) tree; }
			 */
    }
  }

  @Override
  public JavaUnparseStyle getStyle() {
    return style;
  }

  @Override
  public SyntaxTreeInterface getTree() {
    return tree;
  }

  @Override
  public boolean isImplicit(IRNode node) {
    // TODO Auto-generated method stub
    return JavaNode.wasImplicit(node);
  }
}
