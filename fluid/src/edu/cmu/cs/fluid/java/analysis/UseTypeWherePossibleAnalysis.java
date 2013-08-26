package edu.cmu.cs.fluid.java.analysis;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class UseTypeWherePossibleAnalysis {

  private static final Logger LOG = SLLogger
      .getLogger("FLUID.analysis.useTypeWherePossible");

  private final IBinder binder;

  /**
   * Filled in by UseTypeWherePossibleAnnotation
   */
  public static Map<IRNode, Boolean> promiseToAnyResults = new HashMap<IRNode, Boolean>(); // of
                                                                                            // IRNode
                                                                                            // to
                                                                                            // Boolean

  /**
   * Map from local declarations to the useTypeWherePossible design intent for
   * their declared type. A declaration is only put within this map if the field
   * or local variable has useTypeWherePossible design intent.
   */
  private Map<IRNode, IRNode> toPromise; // of IRNode -> IRNode

  private Map<IRNode, IRNode> toCurrentClass; // of IRNode -> IRNode

  /**
   * Map from local declarations to a Boolean indicating if variable use is
   * inconsistant with the useTypeWherePossible promise for its declared type. A
   * <code>false</code> value indicates it is, a <code>true</code> value
   * inticates the declartion's type can be changed to the type specified by the
   * useTypeWherePossible promise.
   * 
   * A declaration will only exist as a key value in this map if its declared
   * type has an useTypeWherePossible promise associated with it (i.e., it
   * exists in the toPromise field above)
   */
  private Map<IRNode, Boolean> changeType; // of IRNode -> Boolean

  private static class UseInformation {

    /**
     * The field or local variable use within the fAST.
     */
    IRNode use;

    /**
     * True if the use is allowed by the type specified by the
     * 
     * @useTypeWherePossible promise, false otherwise.
     */
    boolean availableInPromisedUseType;
  }

  /**
   * Tracks, for positive and negative assurance reporting, use of variables
   * with
   * 
   * @useTypeWherePossible design intent.
   * 
   * The IRNode key should be the FieldDeclaration for private fields and the
   * "?TODO" for local variables.
   */
  private Map<IRNode, Set<UseInformation>> useTracking; // of IRNode -> (Set of
                                                        // UseInformation)

  /**
   * Utility routine to reset fields used to track per-compiliaton unit analysis
   * information.
   */
  private void resetFieldsForCompilationUnit() {
    toPromise = new HashMap<IRNode, IRNode>();
    toCurrentClass = new HashMap<IRNode, IRNode>();
    changeType = new HashMap<IRNode, Boolean>();
    useTracking = new HashMap<IRNode, Set<UseInformation>>();
  }

  private static IRNode getParent(final IRNode n) {
    IRNode result = JJNode.tree.getParentOrNull(n);
    // strip off junk like ((((name))))
    if (ParenExpression.prototype.includes(getOperator(result))) {
      return getParent(result);
    }
    return result;
  }

  private static Operator getOperator(final IRNode n) {
    return JJNode.tree.getOperator(n);
  }

  private IRNode getBinding(final IRNode n) {
    return binder.getBinding(n);
  }
  
  private IJavaType getJavaType(final IRNode n) {
    return binder.getJavaType(n);
  }

  /**
   * A utility method to extract the base type of an array type.
   * 
   * @param type
   *          the type that might (or might not) be an array
   * @return the type (untouched) or the base type if the type is an array
   */
  private IRNode getBaseType(final IRNode type) {
    Operator op = getOperator(type);
    if (ArrayDeclaration.prototype.includes(op)) {
      return ArrayDeclaration.getBase(type);
    } else if (ArrayType.prototype.includes(op)) {
      return ArrayType.getBase(type);
    }
    return type;
  }

  /**
   * Given a type this method returns the identifier for the type.
   * 
   * @param type
   *          an IRNode which is either a ClassDeclaration or an
   *          InterfaceDeclaration
   * @return the identifier for the type or "(unknown)"
   */
  private String getTypeName(IRNode type) {
    String result = "(unknown)";
    final Operator op = getOperator(type);
    if (ClassDeclaration.prototype.includes(op)) {
      result = ClassDeclaration.getId(type);
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      result = InterfaceDeclaration.getId(type);
    } else if (Type.prototype.includes(op)) {
      result = DebugUnparser.toString(type);
    }
    return result;
  }

  /**
   * Taking the binding from a FieldRef, this method checks that we indeed are
   * dealing with a field and that the field is private.
   * 
   * @param fieldRefBinding
   *          the binding from a FieldRef
   * @return <code>true</code> if the binding is to a private field,
   *         <code>false</code> otherwise.
   */
  private boolean isPrivateField(final IRNode fieldRefBinding) {
    IRNode fieldDec = getFieldDeclaration(fieldRefBinding);
    if (fieldDec != null) { // couldn't find the declaration
      if (JavaNode.getModifier(fieldDec, JavaNode.PRIVATE)) {
        // System.out.println(" -- private");
        return true;
      }
    }
    return false;
  }

  /**
   * Moving up the fAST, this utility method returns the first node discovered
   * to be either a FieldDeclaration. This fAST node declares a field.
   * 
   * @param fieldRefBinding
   *          the node to start the search
   * @return the discovered FieldDeclaration, or <code>null</code> if one was
   *         not found (considered an error)
   */
  private IRNode getFieldDeclaration(final IRNode fieldRefBinding) {
    IRNode fieldDec = fieldRefBinding;
    try {
      while (fieldDec != null) {
        if (FieldDeclaration.prototype.includes(getOperator(fieldDec))) {
          return fieldDec;
        }
        fieldDec = getParent(fieldDec);
      }
    } catch (Exception e) {
      // ignore, as we report the problem below
    }
    LOG.log(Level.SEVERE,
        "@useTypeWherePossible failed to find FieldDeclaration in "
            + DebugUnparser.toString(fieldRefBinding));
    return null;
  }

  /**
   * Moving up the fAST, this utility method returns the first node discovered
   * to be either a DeclStatement or a ParameterDeclaration. These two fAST
   * nodes declare local variables.
   * 
   * @param varRefBinding
   *          the node to start the search
   * @return the discovered DeclStatement or ParameterDeclaration, or
   *         <code>null</code> if one was not found (considered an error)
   */
  private IRNode getDeclStatementOrParameterDeclaration(
      final IRNode varRefBinding) {
    IRNode varDec = varRefBinding;
    try {
      while (varDec != null) {
        if (DeclStatement.prototype.includes(getOperator(varDec))
            || ParameterDeclaration.prototype.includes(getOperator(varDec))) {
          return varDec;
        }
        varDec = getParent(varDec);
      }
    } catch (Exception e) {
      // ignore, as we report the problem below
    }
    LOG.log(Level.SEVERE,
        "@useTypeWherePossible failed to find DeclStatement in "
            + DebugUnparser.toString(varRefBinding));
    return null;
  }

  /**
   * Moving up the fAST, this utility method strips all ArrayRefExpressions from
   * a node.
   * 
   * @param use
   *          the fAST node to strip
   * @return the node provided, or a higher fAST node after skipping all
   *         ArrayRefExpressions up the tree
   */
  private IRNode stripArrayRefExpressions(final IRNode use) {
    IRNode result = use;
    while (true) {
      if (ArrayRefExpression.prototype.includes(getOperator(result))) {
        result = getParent(result);
      } else {
        return result;
      }
    }
  }

  private void recordDeclaration(final IRNode declaration,
      final IRNode curType, final IRNode promise) {
    toPromise.put(declaration, promise);
    toCurrentClass.put(declaration, curType);
    changeType.put(declaration, Boolean.TRUE);
    Set<UseInformation> useSet = new HashSet<UseInformation>();
    useTracking.put(declaration, useSet);
  }

  private void recordResult(final IRNode use, final IRNode declaration,
      final IRNode curType, final IRNode promise, boolean changeType) {
    if (!toPromise.containsKey(declaration)) {
      recordDeclaration(declaration, curType, promise);
    }
    // update result
    boolean resultSoFar = this.changeType.get(declaration).booleanValue()
        && changeType;
    this.changeType.put(declaration, Boolean.valueOf(resultSoFar));
    // record use
    UseInformation useInfo = new UseInformation();
    useInfo.use = use;
    useInfo.availableInPromisedUseType = changeType;
    useTracking.get(declaration).add(useInfo);
  }

  @SuppressWarnings("deprecation")
  private IJavaType getEnclosingType(IRNode method) {
    IRNode td = VisitUtil.getEnclosingType(method);
    return JavaTypeFactory.getMyThisType(td);
  }

  /**
   * Checks if a method subsumes a method in the (desired) promised type or a
   * supertype of the promised type.
   * 
   * @param methodDeclaration
   *          the method to check
   * @param promisedType
   *          the type to check if the method is available within
   * @return <code>true</code> if the method is available in the promised
   *         type, <code>false</code> otherwise
   */
  private boolean doesMethodSubsumeAMethodInPromisedType(
      final IRNode methodDeclaration, final IRNode promisedType) {
    // create a list of types where the method exists
    List<IJavaType> typesContiningMethodList = new LinkedList<IJavaType>();
    // add the type we found the method within (could be the promised type)

    typesContiningMethodList.add(getEnclosingType(methodDeclaration));

    for (Iterator<IBinding> i = binder.findOverriddenMethods(methodDeclaration); i
        .hasNext();) {
      IRNode m = i.next().getNode();
      typesContiningMethodList.add(getEnclosingType(m));
    }
    // list out types found
    final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
    final IJavaType promisedTypeBinding = binder.getJavaType(promisedType);
    for (Iterator<IJavaType> i = typesContiningMethodList.iterator(); i
        .hasNext();) {
      IJavaType type = i.next();
      // System.out.println(" --> method \""
      // + DebugUnparser.toString(methodDeclaration)
      // + "\" subsumes method in \"" + getTypeName(type) + "\"");
      if (promisedTypeBinding == type
          || typeEnv.isSubType(promisedTypeBinding, type)) {
        return true; // the method is available in the promised type
      }
    }
    return false;
  }

  private void noteInterestingUse(final IRNode use, final IRNode declaration,
      final IRNode currentType, final IRNode promisedType) {
    // Is this a method call?
    IRNode parent = stripArrayRefExpressions(getParent(use));
    Operator parentOp = getOperator(parent);
    if (MethodCall.prototype.includes(parentOp)) {
      IRNode mB = getBinding(parent);
      Operator mO = getOperator(mB);
      if (MethodDeclaration.prototype.includes(mO)) {
        System.out
            .println(" -> method invoked: " + MethodDeclaration.getId(mB));
        boolean methodInPromisedType = doesMethodSubsumeAMethodInPromisedType(
            mB, promisedType);
        System.out.println(" -> method available in promised type = "
            + methodInPromisedType);
        recordResult(use, declaration, currentType, promisedType,
            methodInPromisedType);
      }
    }
  }

  /**
   * Examines a field reference to see if it is "interesting" in the sense that
   * useTypeWherePossible design intent exists on the declared type of the field
   * and that the field is private (avoiding whole-program analysis). If the
   * reference is interesting then {@link #noteInterestingUse) is invoked.
   * 
   * @param fieldRef
   *          the field reference
   */
  private void examineFieldRefUse(final IRNode fieldRef) {
    final IRNode fieldRefBinding = getBinding(fieldRef);
    final IRNode fieldDec = getFieldDeclaration(fieldRefBinding); // the field
    // Check to ensure the field is a field and that it is private
    if (isPrivateField(fieldDec)) {
      // Check to ensure the type that declares the field has a valid
      // useTypeWherePossible promise promised type
      IRNode type = FieldDeclaration.getType(fieldDec); // get the field type
      type = getBaseType(type); // if an array, get the base type
      IJavaType jt = getJavaType(type); // bind into the field type
      if (jt instanceof IJavaDeclaredType) {
        IRNode typeBinding = ((IJavaDeclaredType) jt).getDeclaration();
        final IRNode promisedType = null;//UseTypeWherePossibleAnnotation
        	//.getPromisedType(typeBinding); // look for a @useTypeWherePossible
        if (promisedType != null) {
          System.out.println("@useTypeWherePossible interesting field use: \""
              + DebugUnparser.toString(fieldRef) + "\" of type \""
              + getTypeName(typeBinding) + "\" promised type \""
              + getTypeName(promisedType) + "\"");
          noteInterestingUse(fieldRef, fieldDec, typeBinding, promisedType);
        }
      }
    }
  }

  /**
   * Examines a local variable reference to see if it is "interesting" in the
   * sense that useTypeWherePossible design intent exists on the declared type
   * of the local variable. If the reference is interesting then
   * {@link #noteInterestingUse) is invoked.
   * 
   * @param varRef
   *          the variable reference
   */
  private void examineLocalVariableRef(final IRNode varRef) {
    final IRNode varRefBinding = getBinding(varRef);
    // Declaration is either a DeclStatement or a ParameterDeclaration
    final IRNode varDec = getDeclStatementOrParameterDeclaration(varRefBinding);
    // Check to ensure the type of the variable has a valid useTypeWherePossible
    // promise promised type
    IRNode type; // get the declared type of the variable
    if (DeclStatement.prototype.includes(getOperator(varDec))) {
      type = DeclStatement.getType(varDec);
    } else {
      type = ParameterDeclaration.getType(varDec);
    }
    type = getBaseType(type); // if an array, get the base type
    IJavaType jt = getJavaType(type);
    if (jt instanceof IJavaDeclaredType) {
      IRNode typeBinding = ((IJavaDeclaredType) jt).getDeclaration();
      final IRNode promisedType = null;//UseTypeWherePossibleAnnotation
    	  //.getPromisedType(typeBinding); // look for a @useTypeWherePossible
      if (promisedType != null) {
        System.out.println("@useTypeWherePossible interesting variable use: \""
            + DebugUnparser.toString(varRef) + "\" of type \""
            + getTypeName(typeBinding) + "\" promised type \""
            + getTypeName(promisedType) + "\"");
        noteInterestingUse(varRef, varDec, typeBinding, promisedType);
      }
    }
  }

  public UseTypeWherePossibleAnalysis(final IBinder b) {
    binder = b;
  }

  public void analyzeCompilationUnit(final IRNode compUnit) {
    resetFieldsForCompilationUnit();
    final Iterator<IRNode> nodes = JJNode.tree.topDown(compUnit);
    while (nodes.hasNext()) {
      final IRNode node = nodes.next();
      final Operator op = getOperator(node);
      if (FieldDeclaration.prototype.includes(op)) {
        // System.out.println("FieldDeclaration " +
        // DebugUnparser.toString(node));
        // if (JavaNode.getModifier(node, JavaNode.PRIVATE)) {
        // System.out.println(" -- private");
        // }
      } else if (ParameterDeclaration.prototype.includes(op)) {
        // System.out.println("ParameterDeclaration "
        // + DebugUnparser.toString(node));
      } else if (FieldRef.prototype.includes(op)) {
        try {
          examineFieldRefUse(node);
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "@useTypeWherePossible encountered a problem"
              + " examining the field reference "
              + DebugUnparser.toString(node) + " within compilation unit:\n"
              + DebugUnparser.toString(compUnit) + "\n", e);
        }
      } else if (VariableUseExpression.prototype.includes(op)) {
        try {
          examineLocalVariableRef(node);
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "@useTypeWherePossible encountered a problem"
              + " examining the local variable use "
              + DebugUnparser.toString(node) + " within compilation unit:\n"
              + DebugUnparser.toString(compUnit) + "\n", e);
        }
      }
    }
    // report results
    for (Iterator<IRNode> i = toPromise.keySet().iterator(); i.hasNext();) {
      IRNode declaration = i.next();
      IRNode promise = toPromise.get(declaration);
      IRNode curClass = toCurrentClass.get(declaration);
      String modelName = "for variables of type " + getTypeName(curClass)
          + " @useTypeWherePossible " + getTypeName(promise);
      // hack for now
      // reporter.reportModel(modelName, "type use model recognized", modelName,
      // REPORT_CATEGORY, JavaNode.getSrcRef(curClass).getLineNumber());
      // if (changeType.get(declaration).booleanValue()) {
      // System.out.println(" ---> reporting a problem");
      // reporter
      // .reportProblem(
      // modelName,
      // "private field(s)/local variable(s) declared using an overspecific
      // type",
      // "\"" + DebugUnparser.toString(declaration)
      // + "\" should be declared to be of type "
      // + getTypeName(toPromise.get(declaration)), REPORT_CATEGORY,
      // JavaNode.getSrcRef(declaration).getLineNumber());
      // } else {
      // System.out.println(" ---> reporting + assurance");
      // reporter
      // .reportAssurance(
      // modelName,
      // "private field(s)/local variable(s) declared to be the appropriate
      // type",
      // "\"" + DebugUnparser.toString(declaration)
      // + "\" is declared to be the appropriate type",
      //                REPORT_CATEGORY, JavaNode.getSrcRef(declaration)
      //                    .getLineNumber());
      //      }
    }
  }
}
