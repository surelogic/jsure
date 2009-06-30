package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.AbstractDropFactory.DependencyType;
import edu.cmu.cs.fluid.java.promise.UsedBySpecification;
import edu.cmu.cs.fluid.promise.IPromiseCheckReport;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.sea.drops.promises.UsedByPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class UsedByAnnotation extends AbstractPromiseAnnotation {

  private UsedByAnnotation() {
    // private constructor for singleton creation
  }

  private static final UsedByAnnotation INSTANCE = new UsedByAnnotation();

  public static final UsedByAnnotation getInstance() {
    return INSTANCE;
  }

  private static SlotInfo<IRNode> usedBySI;

  private static IDropFactory<UsedByPromiseDrop, Object> usedByDropFactory = 
    new AbstractDropFactory<UsedByPromiseDrop, Object>(DependencyType.ONLY_CU, "UsedBy") {
    @Override
    public UsedByPromiseDrop newDrop(final IRNode declNode, Object val) {
      // if it should exist, create it
      IRNode usedByTargets = getUsedBySpec(declNode);
      if (usedByTargets != null) {
        // Create a promise drop for this promise annotation
        UsedByPromiseDrop drop = new UsedByPromiseDrop();
        drop.setCategory(JavaGlobals.USES_CAT);
        drop.setNode(usedByTargets);
        drop.setMessage(Messages.UsedByAnnotation_usedByDrop,
            getTypeList(usedByTargets), JavaNames.getTypeName(declNode));
        return drop;
      }
      return null;
    }
  };

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] { new UsedBy_ParseRule(), };
  }

  public static IRNode getUsedBySpec(IRNode declNode) {
    return getXorNull_filtered(usedBySI, declNode);
  }

  public static void setUsedBySpec(IRNode declNode, IRNode usedBySpec) {
    setX_mapped(usedBySI, declNode, usedBySpec);
    getUsedByDrop(declNode); // create drop
  }

  public static UsedByPromiseDrop getUsedByDrop(IRNode declNode) {
    return getDrop(usedByDropFactory, declNode);
  }

  private static String getTypeList(IRNode usedByTargets) {
    String result = ""; //$NON-NLS-1$
    Iterator<IRNode> typeList = UsedBySpecification
        .getTypesIterator(usedByTargets);
    int count = 0;
    while (typeList.hasNext()) {
      result += (count++ > 0 ? ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
          + JavaNames.getTypeName(typeList.next());
    }
    return result;
  }

  /*****************************************************************************
   * Private implementation
   ****************************************************************************/
  private boolean checkReferencedType(IPromiseCheckReport report,
      IRNode promisedType) {
    try {
      // check if we can bind to the type (i.e., the type exists)
      IRNode promisedTypeBinding = binder.getBinding(promisedType);
      // System.out.println("checking type " +
      // getTypeName(promisedTypeBinding));
      if (promisedTypeBinding == null) {
        report.reportError("The type \"" + DebugUnparser.toString(promisedType) //$NON-NLS-1$
            + "\" specified by @usedBy does not exist" //$NON-NLS-1$
            + " (or is not visible)", promisedType); //$NON-NLS-1$
        return false;
      }
      // looks OK!
      return true;
    } catch (Throwable t) {
      report.reportError("caught while checking the type \"" //$NON-NLS-1$
          + DebugUnparser.toString(promisedType) + "\" specified in @usedBy: " //$NON-NLS-1$
          + t.getMessage(), promisedType);
    }
    return false;
  }

  /**
   * Check the
   * 
   * @usedBy annotation has reasonable types
   */
  private boolean check(IPromiseCheckReport report, IRNode promisedFor,
      IRNode allowedSubtypes) {
    Iterator<IRNode> typeList = UsedBySpecification
        .getTypesIterator(allowedSubtypes);
    boolean result = true;
    while (typeList.hasNext()) {
      result &= checkReferencedType(report, typeList.next());
    }
    return result;
  }

  /*****************************************************************************
   * Parse rule for @usedBy
   ****************************************************************************/
  class UsedBy_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {

    public UsedBy_ParseRule() {
      super("UsedBy", NODE, false, declOps, declOps); //$NON-NLS-1$
    }

    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      usedBySI = si;
      return new TokenInfo<IRNode>("Used by", si, "usedBy"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      setUsedBySpec(n, result);
      return true;
    }

    @Override
    public boolean checkSanity(Operator pop, IRNode promisedFor,
        IPromiseCheckReport report) {
      // Check type list allowed to be used via a @usedBy promise
      // (a NamedType() within edu.cmu.csfluid.eclipse.promise.PromiseX.jjt)
      IRNode usedByTargets = getXorNull(usedBySI, promisedFor);
      if (usedByTargets == null) {
        return true;
      } else {
        // System.out.println("@usedBy: " +
        // DebugUnparser.toString(promisedFor));
        return check(report, promisedFor, usedByTargets);
      }
    }
  }
}