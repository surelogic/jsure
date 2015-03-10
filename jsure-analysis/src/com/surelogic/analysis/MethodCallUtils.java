package com.surelogic.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SomeFunctionCall;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class MethodCallUtils {
  public static final class EnclosingRefs {
    public final IRNode immediatelyEnclosingInstanceRef;
    public final IRNode immediatelyEnclosingInstanceActual;
    public final IRNode immediatelyEnclosingInstanceWithRespectToSRef;
    public final IRNode immediatelyEnclosingInstanceWithRespectToSAcutal;
    public final Map<IRNode, IRNode> rest = new HashMap<IRNode, IRNode>();
    
    public EnclosingRefs(final IRNode instanceRef, final IRNode instanceActual,
        final IRNode sRef, final IRNode sActual,
        final IBinder binder, final IRNode mdecl, final IRNode callingMethodDecl) {
      immediatelyEnclosingInstanceRef = instanceRef;
      immediatelyEnclosingInstanceActual = instanceActual;
      immediatelyEnclosingInstanceWithRespectToSRef = sRef;
      immediatelyEnclosingInstanceWithRespectToSAcutal = sActual;

      /* See the comments in constructFormalToActualMap() about copying
       * qualified receivers across contexts.
       */
      /* instanceRef != null implies instanceActual is not null, but we 
       * can have cases where instanceRef == null and instanceActual != null.
       */
      final boolean enclosingInstanceIsThis =
        (instanceRef != null) && isReceiverNode(instanceActual);
      /* Okay if there isn't an enclosing receiver w.r.t. S, or if that 
       * object is a receiver object.
       */
      final boolean enclosingInstanceWRTSIsThis = 
        (sActual == null) || isReceiverNode(sActual);
      if (enclosingInstanceIsThis && enclosingInstanceWRTSIsThis) {
        // Get the qualified receivers of the called method
        for (final IRNode qrn : JavaPromise.getQualifiedReceiverNodes(mdecl)) {
          final IRNode typeNameAST = QualifiedReceiverDeclaration.getBase(qrn);
          final String typeName    = JavaNames.unparseType(typeNameAST);
          // Get the corresponding qualified receiver in the calling context
          final IRNode callingQRN = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(callingMethodDecl, typeName);
          /* Qualified receiver might not exist in the calling context, in which
           * case we do not map the receiver
           */
          if (callingQRN != null) rest.put(qrn, callingQRN);
        }
      }
    }
    
    /** 
     * Given an expression, return the immediately enclosing object from the 
     * calling context if the expression is the immediately enclosing
     * object from the anonymous class body; return the immediately enclosing 
     * Instance with respect to s from the calling context if the expression is 
     * the immediately enclosing instance with respect to s in the anonymous
     * class body.  Otherwise, return <code>null</code>. 
     * @param ref
     * @return
     */
    public IRNode replace(final IRNode ref) {
      /* We check against the enclosing instance with respect to S first,
       * because it might be that the enclosing instance with respect to S and
       * enclosing instance are referred to by the same qualified receiver. In
       * this case, we want to replace it by the actual enclosing instance
       * with respect to S.
       */ 
      if (ref == immediatelyEnclosingInstanceWithRespectToSRef) {
        return immediatelyEnclosingInstanceWithRespectToSAcutal;
      } else if (ref == immediatelyEnclosingInstanceRef) {
        return immediatelyEnclosingInstanceActual;
      } else {
        return rest.get(ref);
//        return null;
      }
    }
  }
  
  
  
  private MethodCallUtils() {
    // Empty private constructor to prevent class from being instantiated 
  }
  
  
  
  private static boolean isReceiverNode(final IRNode testMe) {
    final Operator testMeOp = JJNode.tree.getOperator(testMe);
    return ThisExpression.prototype.includes(testMeOp)
        || SuperExpression.prototype.includes(testMeOp)
        || QualifiedThisExpression.prototype.includes(testMeOp)
        || ReceiverDeclaration.prototype.includes(testMeOp)
        || QualifiedReceiverDeclaration.prototype.includes(testMeOp);
  }
  
  
  
  /**
   * Build a map from formals to actuals for a method call. Handles
   * substitutions for qualified receivers based on JLS3 15.9.2 "Determining
   * Enclosing Instances" and JLS3 8.8.7.1 "Explicit constructor invocation".
   * 
   * <p>
   * This version of the method is useful for callers that already have a
   * reference to the method declaration because they are using it for other
   * purposes. So why should we look it up a second time?
   * 
   * @param call
   *          The node for the invocation expression.
   * @param mdecl
   *          The node for the method declaration obtained from binding call.
   * @param callingMethodDecl
   *          The node for the declaration of the method that contains the call.
   * @return <code>Map</code> from formals to actuals. It is not always
   *         possible to map all qualified receivers to specific actual
   *         parameters. Qualified receivers that cannot be mapped are mapped to
   *         <code>null</code>. It is the client's responsibility to behave
   *         appropriately in this case.
   */
  public static Map<IRNode, IRNode> constructFormalToActualMap(
      final IBinder binder, final IRNode call, final IRNode mdecl,
      final IRNode callingMethodDecl) {
    /* We may want to check the parent of the call node to see if it is an
     * OuterObjectSpecifier.  However, if the call node is part of an
     * AnonClassExpression, then we want to check the grandparent of the call
     * node.
     */
    final IRNode potentialOOS;
    {
      final IRNode callParent = JJNode.tree.getParentOrNull(call);
      if (AnonClassExpression.prototype.includes(callParent)) {
        potentialOOS = JJNode.tree.getParentOrNull(callParent);
      } else {
        potentialOOS = callParent;
      }
    }

    // ==== Step 1: map the formal -> actual parameters
    
    // get the formal parameters
    final Operator op = JJNode.tree.getOperator(mdecl);
    final IRNode params = ((SomeFunctionDeclaration) op).get_Params(mdecl);
    final Iterator<IRNode> paramsEnum = Parameters.getFormalIterator(params);

    // get the actual parameters
    final Operator callOp = JJNode.tree.getOperator(call);
    Iterator<IRNode> actualsEnum;
    try {
      actualsEnum = Arguments.getArgIterator(((CallInterface) callOp).get_Args(call));
    } catch(final CallInterface.NoArgs e) {
      actualsEnum = new EmptyIterator<IRNode>();
    }

    // build a table mapping each formal parameter to its actual
    final Map<IRNode, IRNode> table = new HashMap<IRNode, IRNode>();
    while (paramsEnum.hasNext()) {
      table.put(paramsEnum.next(), actualsEnum.next());
    }

    // ==== Step 2: map the receiver; generically map the qualified receivers (enclosing instances)
    IRNode receiverRef = null;
    IRNode receiverActual = null;
    
    /* There are certain common situations where we can map the qualified
     * receivers of the called methods to those of the calling method:
     * 
     * For a method call, we can do this when the receiver of the called method
     * is a receiver of the calling method.  This is the
     * case when the actual receiver of the called method is "this", "super", or
     * a qualified "this".
     * 
     * (More notes on this below as the situation warrants.)
     */
    boolean copyQualifiedReceiversAcrossContexts = false;
    
    /* If we are calling an instance method, we map the receiver of the called
     * method to the actual receiver expression in the method call.
     */
    if (MethodCall.prototype.includes(callOp) && !TypeUtil.isStatic(mdecl)) {
      receiverRef = JavaPromise.getReceiverNodeOrNull(mdecl);
      receiverActual = ((SomeFunctionCall) callOp).get_Object(call);
      // See above comment
      copyQualifiedReceiversAcrossContexts = isReceiverNode(receiverActual);
    }    

    // ==== Step 3: Try to map the immediately enclosing instance 
    
    // Here we follow the rules in JLS3 15.9.2 "Determining Enclosing Instances"
    IRNode enclosingInstanceRef = null;
    IRNode enclosingInstanceActual = null;
    
    if (AnonClassExpression.prototype.includes(callOp)) {
      if (!TypeUtil.occursInAStaticContext(call)) {
        // The anonymous class is a direct inner class of the class that contains the allocation expression 
        final IRNode enclosingType = VisitUtil.getEnclosingType(call);
        // Get the reference to the enclosing instance in the called method
        enclosingInstanceRef = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(mdecl, enclosingType);
        /* Here we have an interesting problem.  The anonymous class expression binds
         * to the declaration of the super-class constructor being invoked, e.g.,
         * the declaration of "Foo()" in "new Foo() { ... }".  This declaration
         * might be in an outer scope that doesn't know anything about the 
         * enclosing type of the anonymous class expression.  So it is possible
         * that enclosingInstnaceRef will be null.  This is okay, because 
         * annotations on that super constructor obviously cannot refer to this
         * type either, and that is what these substitutions are being used for.
         * So we just leave the enclosing instance references null when this 
         * happens.
         */
        if (enclosingInstanceRef != null) {
          enclosingInstanceActual = JavaPromise.getReceiverNodeOrNull(callingMethodDecl);
        }
      } else {
        // There is no immediately enclosing instance
      }
    } else if (NewExpression.prototype.includes(callOp)) {
      // typeC is the type being instantiated
      final IRNode typeC = binder.getBinding(NewExpression.getType(call));
      
      if (TypeUtil.isInner(typeC)) {
        /* [*] When we have a new expression, we can map the qualified receivers of
         * the called methods to those of the calling method when the actual 
         * enclosing object reference is a receiver of the
         * calling context.  This occurs when the enclosing object is "this",
         * "super", or a qualified "this".
         */
        
        // typeC is a direct inner class of enclosingType
        final IRNode enclosingType = VisitUtil.getEnclosingType(typeC);
        
        if (TypeUtil.isLocal(typeC)) {
          if (!TypeUtil.occursInAStaticContext(typeC)) {
            // Get the reference to the enclosing instance in the called method
            enclosingInstanceRef = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(mdecl, enclosingType);
            enclosingInstanceActual = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(callingMethodDecl, enclosingType);
            // See [*] above
            copyQualifiedReceiversAcrossContexts = isReceiverNode(enclosingInstanceActual);
          } else {
            // There is no immediately enclosing instance
          }
        } else if (TypeUtil.isMember(typeC)) { // really this test isn't necessary, but I have it for clarity
          // Get the reference to the enclosing instance in the called method
          enclosingInstanceRef = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(mdecl, enclosingType);
          // Get the actual enclosing object in the calling context
          if (OuterObjectSpecifier.prototype.includes(potentialOOS)) {
            enclosingInstanceActual = OuterObjectSpecifier.getObject(potentialOOS);
          } else {
            enclosingInstanceActual = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(callingMethodDecl, enclosingType);
          }
          assert enclosingInstanceActual != null; // Something went wrong if the actual is null, Java rules say it must exist
          // See [*] above
          copyQualifiedReceiversAcrossContexts = isReceiverNode(enclosingInstanceActual);
        }
      }
    }
        
    // ==== Step 4: Try to map the immediately enclosing instance with respect to the superclass more specifically
    
    IRNode enclosingInstanceWRTSRef = null;
    IRNode enclosingInstanceWRTSActual = null;
    
    // Here we follow the rules in JLS3 15.9.2 "Determining Enclosing Instances"
    // plus JLS 8.8.7.1 "Explicit constructor invocation"
    
    /* [+] When creating an object that has an enclosing instance with respect to
     * S, we can map the qualified receivers of the called methods to those of
     * the calling method when 
     *   (1) the actual enclosing object reference is a
     *       receiver of the calling context.
     *   (2) the actual enclosing object with respect to S is a receiver of the
     *       calling context.
     */
    
    // First JLS 8.8.7.1 "Explicit constructor invocation"
    if (ConstructorCall.prototype.includes(callOp)) {
      if (OuterObjectSpecifier.prototype.includes(potentialOOS)) { // o. super(...)
        // typeS is the super type S being constructed
        final IRNode typeS = VisitUtil.getEnclosingType(mdecl);
        // typeS is a direct inner class of typeSO -- this must exist, because the code compiles
        final IRNode typeSO = VisitUtil.getEnclosingType(typeS);
        // Get the reference to the immediately enclosing instance with respect to typeS
        enclosingInstanceWRTSRef = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(mdecl, typeSO);
        // The actual enclosing instance with respect to typeS is specified by the OuterObjectSpecifier
        enclosingInstanceWRTSActual = OuterObjectSpecifier.getObject(potentialOOS);
        // (See [+]): The receiver is "this"
        copyQualifiedReceiversAcrossContexts = isReceiverNode(enclosingInstanceWRTSActual);
      } else {
        // (See [+]) The receiver is "this", and there is no enclosing object with respect to s
        copyQualifiedReceiversAcrossContexts = true;
      }
    } else if (AnonClassExpression.prototype.includes(callOp)) { // Now JLS3 15.9.2 "Determining Enclosing Instances"
      // typeS is the superclass of the anonymous class
      final IRNode typeS = binder.getBinding(AnonClassExpression.getType(call));
      if (TypeUtil.isInner(typeS)) {
        // typeO is the lexically innermost enclosing class of typeS
        final IRNode typeO = VisitUtil.getEnclosingType(typeS);
        if (TypeUtil.isLocal(typeS)) {
          if (!TypeUtil.occursInAStaticContext(typeS)) {
            // Get the reference to the immediately enclosing instance with respect to typeS
            enclosingInstanceWRTSRef = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(mdecl, typeO);
            // The actual enclosing instance with respect to typeS is implied by context
            enclosingInstanceWRTSActual = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(callingMethodDecl, typeO);
            assert enclosingInstanceWRTSActual != null; // Something went wrong if the actual is null, Java rules say it must exist
            // (See [+]) The enclosing instance is "this"
            copyQualifiedReceiversAcrossContexts = isReceiverNode(enclosingInstanceWRTSActual);
          } else {
            // No immediately enclosing instance with respect to typeS
          }
        } else { // typeS is an inner member class
          // Get the reference to the immediately enclosing instance with respect to typeS
          enclosingInstanceWRTSRef = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(mdecl, typeO);
          if (OuterObjectSpecifier.prototype.includes(potentialOOS)) { // o. super(...) { ... }
            // The actual enclosing instance with respect to typeS is specified by the OuterObjectSpecifier
            enclosingInstanceWRTSActual = OuterObjectSpecifier.getObject(potentialOOS);
          } else {
            // The actual enclosing instance with respect to typeS is implied by context
            enclosingInstanceWRTSActual = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(callingMethodDecl, typeO);
          }
          assert enclosingInstanceWRTSActual != null; // Something went wrong if the actual is null, Java rules say it must exist
          // (See [+]) The enclosing instance is "this"
          copyQualifiedReceiversAcrossContexts = isReceiverNode(enclosingInstanceWRTSActual);
        }
      } else {
        // nothing to do
      }
    }
    
    if (copyQualifiedReceiversAcrossContexts) {        
      // Get the qualified receivers of the called method
      for (final IRNode qrn : JavaPromise.getQualifiedReceiverNodes(mdecl)) {
    	final IRNode typeNameAST = QualifiedReceiverDeclaration.getBase(qrn);
        final String typeName    = JavaNames.unparseType(typeNameAST);
        // Get the corresponding qualified receiver in the calling context
        final IRNode callingQRN = JavaPromise.getQualifiedReceiverNodeByName_Bug1392WorkAround(callingMethodDecl, typeName);
        // Qualified receiver might not exist in the calling context
        if (callingQRN != null) table.put(qrn, callingQRN);
      }
    }
    if (receiverRef != null) table.put(receiverRef, receiverActual);
    if (enclosingInstanceRef != null) table.put(enclosingInstanceRef, enclosingInstanceActual);
    if (enclosingInstanceWRTSRef != null) table.put(enclosingInstanceWRTSRef, enclosingInstanceWRTSActual);            

    return table;
  }
  
  
  
  /**
   * Get the immediately enclosing instance and the immediately enclosing
   * instance with respect to s for the given anonymous class expression.
   * 
   * @param binder
   *          The binder to use
   * @param anonClass
   *          The anonymous class expression.  May be an AnonClassExpression
   *          or an EnumConstantClassExpression.
   * @param superClass
   *          The super class of <code>anonClass</code>.  Taken as a parameter
   *          because of the differences between AnonClassExpression
   *          and EnumConstantClassExpression.
   * @param theReceiverNode
   *          The receiver node of the calling context, or code
   *          <code>null</code> if the calling context is static.
   * @param enclosingMethod
   *          The enclosing method of the anonymous class expression. The should
   *          be {@code PromiseUtil.getEnclosingMethod(anonClass)}. We make it
   *          a parameter to the method because the calling context may already
   *          have this value available, and thus we can avoid the lookup.
   */
  public static EnclosingRefs getEnclosingInstanceReferences(
      final IBinder binder, final ThisExpressionBinder thisExprBinder,
      final IRNode anonClass, final IRNode superClass, final IRNode theReceiverNode,
      final IRNode enclosingMethod) {
    final IRNode anonClassInitMethod = JavaPromise.getInitMethodOrNull(anonClass);
    
    /* Get the qualified receiver that refers to the "immediately enclosing instance"
     * of the object being created as used within the body of the anonymous class.
     * In the context of the caller of the anonymous class constructor, the
     * immediately enclosing instance is the current receiver, unless the
     * anonymous class expression occurs in a static context.
     */
    final IRNode immediatelyEnclosingInstanceRef;
    final IRNode immediatelyEnclosingInstanceActual;
    if (TypeUtil.occursInAStaticContext(anonClass)) {
      immediatelyEnclosingInstanceRef = null;
      immediatelyEnclosingInstanceActual = null;
    } else {
      immediatelyEnclosingInstanceRef =
        JavaPromise.getQualifiedReceiverNodeByName(
            anonClassInitMethod, VisitUtil.getEnclosingType(anonClass));
      immediatelyEnclosingInstanceActual = theReceiverNode;
    }
    
    /* Get the qualified receiver that refers to the "immediately enclosing instance with respect to S"
     * of the object being created as used within the body of the anonymous class.  Null
     * if the superclass is not an inner class.
     * See JLS3 15.9.2 "Determining Enclosing Instances".  This duplicates 
     * code in constructFormalToActualMap(), but the code here is more specialized to
     * take advantage of contextual information.
     */
    final IRNode immediatelyEnclosingInstanceWithRespectToSRef;
    final IRNode immediatelyEnclosingInstanceWithRespectToSAcutal;
    
    // typeS is the superclass of the anonymous class
//    final IRNode typeS = binder.getBinding(AnonClassExpression.getType(anonClass));
    final IRNode typeS = superClass;
    if (TypeUtil.isInner(typeS)) {
      // typeO is the lexically innermost enclosing class of typeS
      final IRNode typeO = VisitUtil.getEnclosingType(typeS);
      if (TypeUtil.isLocal(typeS)) {
        if (!TypeUtil.occursInAStaticContext(typeS)) {
          immediatelyEnclosingInstanceWithRespectToSRef = 
            JavaPromise.getQualifiedReceiverNodeByName(anonClassInitMethod, typeO);
          immediatelyEnclosingInstanceWithRespectToSAcutal = 
            JavaPromise.getQualifiedReceiverNodeByName(enclosingMethod, typeO);
          // Something went wrong if the actual is null, Java rules say it must exist
          assert immediatelyEnclosingInstanceWithRespectToSAcutal != null; 
        } else {
          // No immediately enclosing instance with respect to typeS
          immediatelyEnclosingInstanceWithRespectToSRef = null;
          immediatelyEnclosingInstanceWithRespectToSAcutal = null;
        }
      } else { // typeS is an inner member class
        immediatelyEnclosingInstanceWithRespectToSRef =
          JavaPromise.getQualifiedReceiverNodeByName(anonClassInitMethod, typeO);
        final IRNode callParent = JJNode.tree.getParentOrNull(anonClass);
        if (OuterObjectSpecifier.prototype.includes(callParent)) { // o. super(...) { ... }
          // The actual enclosing instance with respect to typeS is specified by the OuterObjectSpecifier
          /* Need to bind the receiver, if any.  We cannot wait to do it
           * later at the code point where the substitution is made, because
           * it may be in different method context that has a different set of
           * receivers.  
           */
          immediatelyEnclosingInstanceWithRespectToSAcutal = 
            thisExprBinder.bindThisExpression(  
                OuterObjectSpecifier.getObject(callParent));
        } else {
          // The actual enclosing instance with respect to typeS is implied by context
          immediatelyEnclosingInstanceWithRespectToSAcutal = JavaPromise.getQualifiedReceiverNodeByName(enclosingMethod, typeO);
        }
        assert immediatelyEnclosingInstanceWithRespectToSAcutal != null; // Something went wrong if the actual is null, Java rules say it must exist
      }
    } else {
      // nothing to do
      immediatelyEnclosingInstanceWithRespectToSRef = null;
      immediatelyEnclosingInstanceWithRespectToSAcutal = null;
    }
    
    return new EnclosingRefs(
        immediatelyEnclosingInstanceRef, immediatelyEnclosingInstanceActual,
        immediatelyEnclosingInstanceWithRespectToSRef, immediatelyEnclosingInstanceWithRespectToSAcutal,
        binder, anonClassInitMethod, enclosingMethod);
  }
}
