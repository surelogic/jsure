package edu.cmu.cs.fluid.java.analysis;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

@SuppressWarnings("unchecked")
public final class UsesAnalysis {

  private static final Logger LOG = SLLogger
      .getLogger("FLUID.analysis.UsesPossible");

  private static final String REPORT_CATEGORY = "Program Structure";

  private final IBinder binder;

  /**
   * Filled in by UseTypeWherePossibleAnnotation
   */
  public static Map promiseToAnyResults = new HashMap(); // of IRNode to
                                                          // Boolean

  private Map toCurrentClass; // of IRNode -> IRNode

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
  private Map changeType; // of IRNode -> Boolean

  /**
   * Utility routine to reset fields used to track per-compiliaton unit analysis
   * information.
   */
  private void resetFieldsForCompilationUnit() {
    toCurrentClass = new HashMap();
    changeType = new HashMap();
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

  public UsesAnalysis(final IBinder b) {
    binder = b;
  }

  public void analyzeCompilationUnit(final IRNode compUnit) {
    resetFieldsForCompilationUnit();
    final Iterator<IRNode> nodes = JJNode.tree.topDown(compUnit);
    while (nodes.hasNext()) {
      final IRNode node = nodes.next();
      final Operator op = getOperator(node);
      if (ClassDeclaration.prototype.includes(op)) {
        System.out.println("ClassDeclaration " + DebugUnparser.toString(node));
        try {
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "UsesAnalysis encountered a problem"
              + " examining the ClassDeclaration "
              + DebugUnparser.toString(node) + " within compilation unit:\n"
              + DebugUnparser.toString(compUnit) + "\n", e);
        }
      } else if (InterfaceDeclaration.prototype.includes(op)) {
        System.out.println("InterfaceDeclaration "
            + DebugUnparser.toString(node));
        try {
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "UsesAnalysis encountered a problem"
              + " examining the InterfaceDeclaration "
              + DebugUnparser.toString(node) + " within compilation unit:\n"
              + DebugUnparser.toString(compUnit) + "\n", e);
        }
      }
    }
    // report results
  }
}