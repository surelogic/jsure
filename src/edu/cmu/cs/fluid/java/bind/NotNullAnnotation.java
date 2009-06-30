package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotImmutableException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.ir.SlotInfoWrapper;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.StatementExpressionList;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.promise.IPromiseCheckReport;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.promise.IPromiseStorage;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.promises.NotNullPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class NotNullAnnotation extends AbstractPromiseAnnotation {

  static SlotInfo<Boolean> notNullSI;

  static SlotInfo<Boolean> wrappedNotNullSI;

  private static DefaultDropFactory<NotNullPromiseDrop> notNullSIFactory = 
    new DefaultDropFactory<NotNullPromiseDrop>("NotNull") {
    @Override
    public NotNullPromiseDrop newDrop(IRNode node, Object val) {
      if (isNotNull(node)) {
        NotNullPromiseDrop drop = new NotNullPromiseDrop();
        drop.setCategory(JavaGlobals.NULL_CAT);
        drop.setMessage(Messages.NotNullAnnotation_notNullDrop, JavaNames
            .getFieldDecl(node), JavaNames.genMethodConstructorName(VisitUtil
            .getEnclosingClassBodyDecl(node)));
        return drop;
      }
      return null;
    }
  };

  private NotNullAnnotation() {
  }

  private static final NotNullAnnotation instance = new NotNullAnnotation();

  public static final NotNullAnnotation getInstance() {
    return instance;
  }

  public static SlotInfo<Boolean> getIsNotNullSlotInfo() {
    if (wrappedNotNullSI == null) {
      wrappedNotNullSI = new SlotInfoWrapper<Boolean>(notNullSI) {
        @Override
        protected void setSlotValue(IRNode node, Boolean newValue)
            throws SlotImmutableException {
          setIsNotNull(node, newValue == Boolean.TRUE);
        }
      };
    }
    return wrappedNotNullSI;
  }

  public static boolean isNotNull(IRNode node) {
    return isX_filtered(notNullSI, node);
  }

  public static NotNullPromiseDrop getNotNullDrop(IRNode node) {
    return getDrop(notNullSIFactory, node);
  }

  public static void setIsNotNull(IRNode node, boolean notNull) {
    setX_mapped(notNullSI, node, notNull);
    getNotNullDrop(node);
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] { new NotNull_ParseRule("NotNull") { //$NON-NLS-1$

      public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
        notNullSI = si;
        return new TokenInfo<Boolean>("NotNull", si, name); //$NON-NLS-1$
      }
    }, };
  }

  abstract class NotNull_ParseRule extends AbstractPromiseParserCheckRule<Boolean> {

    protected NotNull_ParseRule(String tag) {
      super(tag, IPromiseStorage.BOOL, false, fieldMethodDeclOps,
          varDeclOrParamDeclOps, varDeclOrParamDeclOps);
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      boolean rv = true;

      final Iterator<IRNode> e = StatementExpressionList
          .getExprIterator(result);
      if (!e.hasNext()) {
        // Should mark the declaration node
        setIsNotNull(n, true);
        return true;
      }
      while (e.hasNext()) {
        final IRNode expr = e.next();
        final Operator eop = tree.getOperator(expr);

        IRNode nodeToSet = null;
        if (eop instanceof ThisExpression) {
          nodeToSet = JavaPromise.getReceiverNodeOrNull(n);
          if (nodeToSet == null) {
            cb.noteProblem("Couldn't find a receiver node for " //$NON-NLS-1$
                + DebugUnparser.toString(n));
            rv = false;
            continue;
          }
        } else if (eop instanceof VariableUseExpression) {
          nodeToSet = BindUtil.findLV(n, VariableUseExpression.getId(expr));

          if (nodeToSet == null) {
            cb
                .noteProblem("Couldn't find '" + VariableUseExpression.getId(expr) //$NON-NLS-1$
                    + "' as parameter in " + DebugUnparser.toString(n)); //$NON-NLS-1$
            rv = false;
            continue;
          }
        } else {
          cb.noteProblem("Unexpected expression for @" + name + ": " //$NON-NLS-1$ //$NON-NLS-2$
              + DebugUnparser.toString(expr));
          rv = false;
          continue;
        }

        setIsNotNull(nodeToSet, true);
      }
      return rv;
    }

    @Override
    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      if (isNotNull(promisedFor)) {
        // should create it for the receiver (hopefully)
        @SuppressWarnings("unused")
        Drop d = getNotNullDrop(promisedFor);
//        System.out.println("found @notNull on " //$NON-NLS-1$
//            + DebugUnparser.toString(promisedFor));
      }
      return true;
    }
  }
}
