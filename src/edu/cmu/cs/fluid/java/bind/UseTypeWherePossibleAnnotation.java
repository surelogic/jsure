package edu.cmu.cs.fluid.java.bind;

import java.util.Collection;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.IPromiseCheckReport;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.tree.Operator;

@Deprecated
public class UseTypeWherePossibleAnnotation extends AbstractPromiseAnnotation {

  private UseTypeWherePossibleAnnotation() {
    // private constructor for singleton creation
  }

  private static final UseTypeWherePossibleAnnotation INSTANCE = new UseTypeWherePossibleAnnotation();

  public static final UseTypeWherePossibleAnnotation getInstance() {
    return INSTANCE;
  }

  private static SlotInfo<IRNode> useTypeWherePossibleSI;

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[]{new UseTypeWherePossible_ParseRule(),};
  }

  /**
   * Provides the type specified by the @useTypeWhere possible promise for the
   * <code>classNode</code> (class or interface) or <code>null</code> if the
   * promise doesn't exist for that class or interface.
   * 
   * @param classNode the class with the @useTypeWherePossible promise
   * @return the IRNode representing the type recommended or <code>null</code>
   *   if <code>classNode</code> has no @useTypeWherePossible promise
   */
  public static IRNode getPromisedType(IRNode classNode) {
    return getXorNull_filtered(useTypeWherePossibleSI, classNode);
  }

  /**
   * Sets the recommended type for "classNode" to "typeNode".
   * 
   * @param classNode the class with the @useTypeWherePossible promise
   * @param promisedTypeNode the type recommended by @useTypeWherePossible
   */
  public static void setPromisedType(IRNode classNode, IRNode promisedTypeNode) {
    setX_mapped(useTypeWherePossibleSI, classNode, promisedTypeNode);
  }

  /* ***********************
   * Private implementation
   * ***********************/
  /**
   * Check that the promised type from an @useTypeWherePossible promise is
   * reasonable.
   */
  @SuppressWarnings("deprecation")
  private boolean checkPromisedType(IPromiseCheckReport report,
      IRNode promisedFor, IRNode promisedType) {
    try {
      // check if we can bind to the type (i.e., the type exists)
      IJavaType promisedForType = JavaTypeFactory.convertIRTypeDeclToIJavaType(promisedFor);
      IJavaType promisedTypeBinding = binder.getJavaType(promisedType);
      if (promisedTypeBinding == null) {
        report.reportError("The type \"" + DebugUnparser.toString(promisedType)
            + "\" specified by @useTypeWherePossible does not exist"
            + " (or is not visible)", promisedType);
        return false;
      }
      // check that the type promised is a supertype of the target type
      final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
      if (!typeEnv.isSubType(promisedForType, promisedTypeBinding)) {
        report.reportError("The type \"" + DebugUnparser.toString(promisedType)
            + "\" specified by @useTypeWherePossible is not a subtype of "
            + getTypeName(promisedFor), promisedType);
        return false;
      }
      // check that the target type is NOT a subclass of Throwable
      IRNode throwable = typeEnv.findNamedType("java.lang.Throwable");
      IJavaType throwableType = JavaTypeFactory.convertIRTypeDeclToIJavaType(throwable);
      if (typeEnv.isSubType(promisedForType, throwableType)) {
        report.reportError(
            "@useTypeWherePossible is not allowed for Java exception types..."
                + "program behaviour could be drastically changed",
            promisedType);
        return false;
      }
      // looks OK!
      return true;
    } catch (Throwable t) {
      report.reportError("caught while checking the type \""
          + DebugUnparser.toString(promisedType)
          + "\" specified in @useTypeWherePossible: " + t.getMessage(),
          promisedType);
    }
    return false;
  }

  /**
   * Given a ClassDeclaration or an InterfaceDeclaration this method returns
   * the identifier for the type.
   * 
   * @param type an IRNode which is either a ClassDeclaration
   *   or an InterfaceDeclaration
   * @return the identifier for the type or "(unknown)"
   */
  private String getTypeName(IRNode type) {
    String result = "(unknown)";
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

  /* ************************************
   * Parse rule for @useTypeWherePossible
   * ************************************/
  class UseTypeWherePossible_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {

    public UseTypeWherePossible_ParseRule() {
      super("UseTypeWherePossible", NODE, false, typeDeclOps, typeDeclOps);
    }

    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      useTypeWherePossibleSI = si;
      return new TokenInfo<IRNode>("Use type where possible", si,
          "useTypeWherePossible");
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb, Collection<IRNode> newResults) {
      setPromisedType(n, result);
      return true;
    }

    @Override
    public boolean checkSanity(Operator pop, IRNode promisedFor,
        IPromiseCheckReport report) {
      // Check promised type from @useTypeWherePossible
      // (a NamedType() within edu.cmu.csfluid.eclipse.promise.PromiseX.jjt)
      IRNode promisedType = getXorNull(useTypeWherePossibleSI, promisedFor);
      if (promisedType == null) {
        return true;
      } else {
        return checkPromisedType(report, promisedFor, promisedType);
      }
    }
  }
}
