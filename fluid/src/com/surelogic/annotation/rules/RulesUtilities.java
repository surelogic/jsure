package com.surelogic.annotation.rules;

import java.util.logging.Level;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

final class RulesUtilities {
  private RulesUtilities() {
    super();
  }



  public static <T extends IAASTRootNode> boolean checkForReferenceType(
      final IAnnotationScrubberContext context, final T a, final String label) {
    final IRNode promisedFor = a.getPromisedFor();
    final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);
    final IJavaType type;
    if (ParameterDeclaration.prototype.includes(promisedForOp)
        || ReceiverDeclaration.prototype.includes(promisedForOp)
        || VariableDeclarator.prototype.includes(promisedForOp)
        || QualifiedReceiverDeclaration.prototype.includes(promisedForOp)) {
      type = context.getBinder(promisedFor).getJavaType(promisedFor);
    } else if (ReturnValueDeclaration.prototype.includes(promisedForOp)) {
      final IRNode method = JavaPromise.getPromisedFor(promisedFor);
      type = context.getBinder(method).getJavaType(method);
    } else {
      AnnotationRules.LOG.log(Level.SEVERE,
          "Unexpected " + promisedForOp.name() + ": " + 
              DebugUnparser.toString(promisedFor), new Throwable());
      return false;
    }
    
    if (type instanceof IJavaPrimitiveType) {
      if (ReturnValueDeclaration.prototype.includes(promisedForOp)) {
        final IRNode mdecl = JavaPromise.getPromisedFor(promisedFor);
        context.reportError(a,
            "Return of method {0} cannot be annotated with @{1}: Primitive return type",
            JavaNames.genRelativeFunctionName(mdecl), label);        
      } else if (ParameterDeclaration.prototype.includes(promisedForOp)) {
        final IRNode mdecl = 
            JJNode.tree.getParent(JJNode.tree.getParent(promisedFor));
        context.reportError(a,
            "Parameter {0} of method {1} cannot be annotated with @{2}: Primitive type",
            ParameterDeclaration.getId(promisedFor), 
            JavaNames.genRelativeFunctionName(mdecl), label);        
      } else { // VariableDeclarator: QualifiedRecievers and Receivers are not primitive
        context.reportError(a,
            "Field {0} cannot be annotated with @{1}: Primitive type",
            VariableDeclarator.getId(promisedFor), label);        
      }
      return false;
    } else if (type == JavaTypeFactory.voidType) {
      // Can only be void if the annotation is on a method return node
      final IRNode mdecl = JavaPromise.getPromisedFor(promisedFor);
      context.reportError(a,
          "Return of method {0} cannot be annotated with @{1}: Void return type",
          JavaNames.genRelativeFunctionName(mdecl), label);
      return false;
    } else {
      return true;
    }
  }
}
