package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.promise.SubtypedBySpecification;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.IPromiseCheckReport;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.promises.SubtypedByPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class SubtypedByAnnotation extends AbstractPromiseAnnotation {

  private SubtypedByAnnotation() {
    // private constructor for singleton creation
  }

  private static final SubtypedByAnnotation INSTANCE = new SubtypedByAnnotation();

  public static final SubtypedByAnnotation getInstance() {
    return INSTANCE;
  }

  private static SlotInfo<IRNode> subtypedBySI;

  private static SlotInfo<SubtypedByPromiseDrop> subtypedByDrop = SimpleSlotFactory.prototype
      .newAttribute(null);

  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] { new SubtypedBy_ParseRule(), };
  }

  public static IRNode getSubtypedBySpec(IRNode declNode) {
    return getXorNull_filtered(subtypedBySI, declNode);
  }

  public static void setSubtypedBySpec(IRNode declNode, IRNode usedBySpec) {
    setX_mapped(subtypedBySI, declNode, usedBySpec);
    getSubtypedByDrop(declNode);
  }

  public static SubtypedByPromiseDrop getSubtypedByDrop(IRNode declNode) {
    SubtypedByPromiseDrop drop = declNode.getSlotValue(subtypedByDrop);
    if (drop != null) {
      return drop;
    } else {
      // if it should exist, create it
      IRNode usedByTargets = getSubtypedBySpec(declNode);
      if (usedByTargets != null) {
        // Create a promise drop for this promise annotation
        drop = new SubtypedByPromiseDrop();
        drop.setCategory(JavaGlobals.USES_CAT);
        drop.setNodeAndCompilationUnitDependency(declNode);
        drop.setMessage(Messages.SubtypedByAnnotation_subtypedByDrop,
            getTypeList(usedByTargets), JavaNames.getTypeName(declNode)); //$NON-NLS-1$
        declNode.setSlotValue(subtypedByDrop, drop);
        drop.setAttachedTo(declNode, subtypedByDrop);
      }
      return null;
    }
  }

  private static String getTypeList(IRNode usedByTargets) {
    String result = ""; //$NON-NLS-1$
    Iterator typeList = SubtypedBySpecification.getTypesIterator(usedByTargets);
    int count = 0;
    while (typeList.hasNext()) {
      result += (count++ > 0 ? ", " : "") //$NON-NLS-1$ //$NON-NLS-2$
          + JavaNames.getTypeName((IRNode) typeList.next());
    }
    return result;
  }

  /*****************************************************************************
   * Private implementation
   ****************************************************************************/
  @SuppressWarnings("deprecation")
  private boolean checkReferencedType(IPromiseCheckReport report,
      IRNode promisedFor, IRNode promisedType) {
    try {
      // check if we can bind to the type (i.e., the type exists)
      IJavaType promisedForType = JavaTypeFactory
          .convertIRTypeDeclToIJavaType(promisedFor);
      IJavaType promisedTypeBinding = binder.getJavaType(promisedType);
      // System.out.println("checking type " +
      // getTypeName(promisedTypeBinding));
      if (promisedTypeBinding == null) {
        report.reportError("The type \"" + DebugUnparser.toString(promisedType) //$NON-NLS-1$
            + "\" specified by @subtypedBy does not exist" //$NON-NLS-1$
            + " (or is not visible)", promisedType); //$NON-NLS-1$
        return false;
      }
      // check that the type promised is a supertype of the target type
      final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
      if (!typeEnv.isSubType(promisedTypeBinding, promisedForType)) {
        report.reportError("The type \"" + DebugUnparser.toString(promisedType) //$NON-NLS-1$
            + "\" specified by @subtypedBy is not a subtype of " //$NON-NLS-1$
            + getTypeName(promisedFor), promisedType);
        return false;
      }
      // looks OK!
      SubtypedByPromiseDrop p = getSubtypedByDrop(promisedFor);
      ResultDrop r = new ResultDrop();
      r.setMessage(Messages.SubtypedByAnnotation_typeDrop, JavaNames
          .getTypeName(promisedType), JavaNames.getTypeName(promisedFor));
      r.setConsistent();
      r.addCheckedPromise(p);
      return true;
    } catch (Throwable t) {
      report.reportError("caught while checking the type \"" //$NON-NLS-1$
          + DebugUnparser.toString(promisedType)
          + "\" specified in @subtypedBy: " + t.getMessage(), promisedType); //$NON-NLS-1$
    }
    return false;
  }

  /**
   * Check the
   * 
   * @subtypedBy annotation has reasonable types
   */
  private boolean check(IPromiseCheckReport report, IRNode promisedFor,
      IRNode allowedSubtypes) {
    Iterator typeList = SubtypedBySpecification
        .getTypesIterator(allowedSubtypes);
    boolean result = true;
    while (typeList.hasNext()) {
      result &= checkReferencedType(report, promisedFor, (IRNode) typeList
          .next());
    }
    return result;
  }

  /**
   * Given a ClassDeclaration or an InterfaceDeclaration this method returns the
   * identifier for the type.
   * 
   * @param type
   *          an IRNode which is either a ClassDeclaration or an
   *          InterfaceDeclaration
   * @return the identifier for the type or "(unknown)"
   */
  private String getTypeName(IRNode type) {
    String result = "(unknown)"; //$NON-NLS-1$
    final Operator op = getOperator(type);
    if (ClassDeclaration.prototype.includes(op)) {
      result = ClassDeclaration.getId(type);
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      result = InterfaceDeclaration.getId(type);
    }
    return result;
  }

  private static Operator getOperator(final IRNode n) {
    return JJNode.tree.getOperator(n);
  }

  /*****************************************************************************
   * Parse rule for
   * 
   * @subtypedBy
   ****************************************************************************/
  class SubtypedBy_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {

    public SubtypedBy_ParseRule() {
      super("SubtypedBy", NODE, false, declOps, declOps); //$NON-NLS-1$
    }

    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      subtypedBySI = si;
      return new TokenInfo<IRNode>("Subtyped by", si, "subtypedBy"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      setSubtypedBySpec(n, result);
      return true;
    }

    @Override
    public boolean checkSanity(Operator pop, IRNode promisedFor,
        IPromiseCheckReport report) {
      // Check type list allowed to be used via a @usedBy promise
      // (a NamedType() within edu.cmu.csfluid.eclipse.promise.PromiseX.jjt)
      IRNode allowedSubtypes = getXorNull(subtypedBySI, promisedFor);
      if (allowedSubtypes == null) {
        return true;
      } else {
        // System.out.println("@subtypedBy: "
        // + DebugUnparser.toString(promisedFor));
        return check(report, promisedFor, allowedSubtypes);
      }
    }
  }
}