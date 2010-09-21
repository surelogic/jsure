/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.StatementExpressionList;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.promise.IPromiseAnnotation;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.promise.IPromiseStorage;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 *  
 */
@Deprecated
public final class TaintednessPromises extends AbstractPromiseAnnotation {
  private TaintednessPromises() {
  }
  
  private static final TaintednessPromises instance = new TaintednessPromises();
  
  public static final IPromiseAnnotation getInstance() {
    return instance;
  }

  static SlotInfo<Boolean> taintedSI;
  static SlotInfo<Boolean> notTaintedSI;
  
  public static boolean isNotTainted(IRNode node) {
    return isXorFalse_filtered(notTaintedSI, node);
  }
  
  public static void setNotTainted(IRNode node, boolean notTainted) {
    // LOG.fine("setting notTainted on " + JavaNode.getInfo(node));
    setX_mapped(notTaintedSI, node, notTainted);
  }
  
  public static boolean isTainted(IRNode node) {
    return isXorFalse_filtered(taintedSI, node);
  }
  
  public static void setTainted(IRNode node, boolean tainted) {
    // LOG.fine("setting tainted on " + JavaNode.getInfo(node));
    setX_mapped(taintedSI, node, tainted);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {
        new Taintedness_ParseRule("Tainted") {
          public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
            taintedSI = si;
            return new TokenInfo<Boolean>("Tainted", si, name);
          }
        },      
        
        new Taintedness_ParseRule("NotTainted") {
          public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
            notTaintedSI = si;
            return new TokenInfo<Boolean>("NotTainted", si, name);
          }
        },      
    };
  }
  
  abstract class Taintedness_ParseRule extends AbstractPromiseParserCheckRule<Boolean> {
    
    protected Taintedness_ParseRule(String tag) {
      // latter should check parameters?
      super(tag, IPromiseStorage.BOOL, false, fieldMethodDeclOps, fieldMethodDeclOps);
    }
    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      boolean rv = true;
            
      final Iterator e = StatementExpressionList.getExprIterator(result);
      boolean haveAny = e.hasNext();
      
      while (e.hasNext()) {
        final IRNode expr = (IRNode) e.next();
        final Operator eop = tree.getOperator(expr);
        
        IRNode nodeToSet = null;
        if (eop instanceof ThisExpression) {
          nodeToSet = JavaPromise.getReceiverNodeOrNull(n);
          if (nodeToSet == null) {
            cb.noteProblem("Couldn't find a receiver node for "
                + DebugUnparser.toString(n));
            rv = false;
            continue;
          }
        } else if (eop instanceof VariableUseExpression) {
          nodeToSet = BindUtil.findLV(n, VariableUseExpression.getId(expr));
          
          if (nodeToSet == null) {
            cb.noteProblem("Couldn't find '" + VariableUseExpression.getId(expr)
                + "' as parameter in " + DebugUnparser.toString(n));
            rv = false;
            continue;
          }
        } else {
          cb.noteProblem("Unexpected expression for @" + name + ": "
              + DebugUnparser.toString(expr));
          rv = false;
          continue;
        }
        
        if (name.equals("tainted")) {
          setTainted(nodeToSet, true);
        } else {
          setNotTainted(nodeToSet, true);
        }
      }
      
      if (!haveAny) {
        // we have a taintedness value with no arguments.  That means that it must apply to
        // the node itself.
        //Operator op = tree.getOperator(n);
        
        if (name.equals("tainted")) {
          setTainted(n, true);
        } else {
          setNotTainted(n, true);
        }
      }
      return rv;
    }
  }
}