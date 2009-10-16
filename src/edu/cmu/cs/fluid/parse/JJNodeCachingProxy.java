/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/parse/JJNodeCachingProxy.java,v 1.2 2007/03/14 19:52:20 chance Exp $*/
package edu.cmu.cs.fluid.parse;

import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A proxy node that caches unchanging node state for efficient access.
 * The only slots cached are ones that are (logically) constant.
 * Of the standard syntax tree node slots, only the location is not
 * constant and thus cannot be cached.
 * @author boyland
 */
public class JJNodeCachingProxy extends AbstractProxyNode {
  private final IRNode underlyingNode;
  private final Operator operator;
  private final IRSequence<IRNode> children;
  private final IRSequence<IRNode> parents;
  
  private static final SlotInfo<Operator> operatorSlotInfo;
  private static final SlotInfo<IRSequence<IRNode>> childrenSlotInfo;
  private static final SlotInfo<IRSequence<IRNode>> parentsSlotInfo;
  
  static {
    JJNode.ensureLoaded();
    try {
      operatorSlotInfo = SlotInfo.findSlotInfo("Parse.SyntaxTree.operator");
      childrenSlotInfo = SlotInfo.findSlotInfo("Parse.Digraph.Children");
      parentsSlotInfo = SlotInfo.findSlotInfo("Parse.Tree.parents");
    } catch (SlotNotRegisteredException e) {
      SLLogger.getLogger().log(Level.SEVERE, "Problem while creating SIs", e);
      throw new Error("Can't load ASTNode");
    }
  }
  
  /**
   * Create a node that caches information from this IRNode
   */
  public JJNodeCachingProxy(IRNode node) {
    underlyingNode = node;
    operator = node.getSlotValue(operatorSlotInfo);
    children = node.getSlotValue(childrenSlotInfo);
    parents = node.getSlotValue(parentsSlotInfo);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.AbstractProxyNode#getIRNode()
   */
  @Override
  protected IRNode getIRNode() {
    return underlyingNode;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException {
    if (si == operatorSlotInfo) return (T)operator;
    else if (si == childrenSlotInfo) return (T)children;
    else if (si == parentsSlotInfo) return (T)parents;
    else return super.getSlotValue(si);
  } 
}
