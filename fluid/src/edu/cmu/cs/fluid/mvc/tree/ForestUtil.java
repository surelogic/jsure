/*
 * Created on Sep 14, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;
import java.util.logging.*;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;


/**
 * @author Edwin
 *
 */
public class ForestUtil {
  /**
   * Logger for this class
   */
  static final Logger LOG = SLLogger.getLogger("MV.tree"); 

  public static interface RootMutator {
    void addRoot(IRNode root);
    Operator getOperator(IRNode n);
  }
  
  private static class Processor extends AbstractNodePromiseProcessor {
    final RootMutator model;

    Processor(RootMutator model) {
      this.model = model;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.annotation.IPromiseHandler#getIdentifier()
     */
    public String getIdentifier() {
      return "AddRoot";
    }
    
    @Override
    protected void process(IRNode root) {
      // Was addRoot()
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("ForestUtil.root = "+DebugUnparser.toString(root));
      }
      if (root != null) {
        model.addRoot(root);
      } else {
        (new Throwable("For stack trace")).printStackTrace();
      }
    }
  }
 
  public static void addPromisesOnTree(final RootMutator model, final IRNode root) {
    addPromisesOnTree(model, JJNode.tree, root);
  }
  
  public static void addPromisesOnTree(final RootMutator model, SyntaxTreeInterface tree, final IRNode root) { 
    final Iterator enm = tree.bottomUp(root); 
    addPromisesOnEnum(model, enm);
  }
  
  public static void addPromisesOnEnum(final RootMutator model, final Iterator enm) {
    while (enm.hasNext()) {
      final IRNode n    = (IRNode) enm.next();
      final Operator op = model.getOperator(n);
      // System.out.println(filter.getClass().getName()+" got op: "+op.name());
      ForestUtil.addPromisesOnNode(model, n, op);         
      /*
      if (MethodDeclaration.prototype.includes(op) ||
          ConstructorDeclaration.prototype.includes(op)) {
        IRNode receiver = JavaPromise.getReceiverNodeOrNull(n);
        addPromisesOnNode(receiver, ReceiverDeclaration.prototype);
      }
      if (MethodDeclaration.prototype.includes(op) ||
          ConstructorDeclaration.prototype.includes(op)) {
        IRNode returnNode = JavaPromise.getReturnNodeOrNull(n);
        addPromisesOnNode(returnNode, ReturnValueDeclaration.prototype);
      }
      */
    }    
  }
  
  private static void addPromisesOnNode(final RootMutator model, final IRNode n, final Operator op) {    
    PromiseFramework.getInstance().processPromises(n, new Processor(model));
    /*
    final Iterator infos = PromiseFramework.getInstance().getTokenInfos(op);
    while (infos.hasNext()) {
      final TokenInfo info = (TokenInfo) infos.next();
      final IRType type    = info.si.getType();
      // System.out.println("Got info for "+info.token.toString());
    
      if (type instanceof IRNodeType) {
        if (n.valueExists(info.si)) {
          addRootIfNonnull(model, n.getSlotValue(info.si));
        }
      } 
      else if (type instanceof IRSequenceType) {
        if (n.valueExists(info.si)) {
          IRSequence seq = (IRSequence) n.getSlotValue(info.si);
          if (seq == null) {
            continue;
          }
          Iterator e  = seq.elements();
          while (e.hasNext()) {
            addRootIfNonnull(model, e.nextElement());
          }
        }
      }
    }    
    */
  } 
}
