/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/AssumeFinalRules.java,v 1.2 2007/11/27 16:30:22 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class AssumeFinalRules extends AnnotationRules {
  public static final String ASSUME_FINAL = "AssumeFinal";
  
  private static final AnnotationRules instance = new AssumeFinalRules();
  
  private static final AssumeFinal_ParseRule assumeFinalRule = new AssumeFinal_ParseRule();
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  public static boolean isAssumedFinal(IRNode vdecl) {
    return getAssumeFinalDrop(vdecl) != null;
  }
  
  public static AssumeFinalPromiseDrop getAssumeFinalDrop(IRNode vdecl) {
    return getBooleanDrop(assumeFinalRule.getStorage(), vdecl);
  }
  
  // Suppress warning because the needed type doesn't yet exist
  @SuppressWarnings("unchecked")
  public static /*Immutable*/PromiseDrop getImmutableDrop(IRNode vdecl) {
    //return getBooleanDrop(immutableRule.getStorage(), vdecl);
    throw new UnsupportedOperationException("no immutable yet");
  }
  
  /**
   * Meant for testing
   */
  public static void setIsUnique(IRNode vdecl, boolean val) {
    if (!val) {
      if (isAssumedFinal(vdecl)) {
        throw new UnsupportedOperationException();
      }
    } else {
      assumeFinalRule.getStorage().add(vdecl, new AssumeFinalPromiseDrop(null));
    }
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, assumeFinalRule);
  }
  
  public static class AssumeFinal_ParseRule 
  extends DefaultBooleanAnnotationParseRule<AssumeFinalNode,AssumeFinalPromiseDrop> {
    public AssumeFinal_ParseRule() {
      super(ASSUME_FINAL, fieldParamDeclOps, AssumeFinalNode.class);
    }
   
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.nothing().getTree();
    }
    @Override
    protected AnnotationLocation translateTokenType(int type, Operator op) {
      AnnotationLocation loc = super.translateTokenType(type, op);
      if (loc == AnnotationLocation.DECL && MethodDeclaration.prototype.includes(op)) {
        return AnnotationLocation.RETURN_VAL;
      }
      return loc;
    }
    @Override
    protected IAASTRootNode makeAAST(int offset) {
      return new AssumeFinalNode(offset);
    }
    @Override
    protected IPromiseDropStorage<AssumeFinalPromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), AssumeFinalPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<AssumeFinalNode> makeScrubber() {
      return new AbstractAASTScrubber<AssumeFinalNode>(this) {
        @Override
        protected PromiseDrop<AssumeFinalNode> makePromiseDrop(AssumeFinalNode a) {
//          AssumeFinalPromiseDrop d = new AssumeFinalPromiseDrop(a);
//          return storeDropIfNotNull(getStorage(), a, d);          
          return storeDropIfNotNull(getStorage(), a, 
              scrubAssumeFinal(getContext(), a));          
        }
      };
    }    
  }
  
  private static AssumeFinalPromiseDrop scrubAssumeFinal(
      final IAnnotationScrubberContext context,
      final AssumeFinalNode a) {
    final IRNode promisedFor = a.getPromisedFor();
    
    // Cannot use TypeUtils.isFinal() because that checks for @AssumeFinal
    boolean isAlreadyFinal = false;
    final Operator op = JJNode.tree.getOperator(promisedFor);
    if (VariableDeclarator.prototype.includes(op)) {
      if (TypeUtil.isInterface(VisitUtil.getEnclosingType(promisedFor))) {
        isAlreadyFinal = true; // declared in an interface
      } else if (JavaNode.getModifier(JJNode.tree.getParent(
          JJNode.tree.getParent(promisedFor)), JavaNode.FINAL)) {
        isAlreadyFinal = true; // declared final
      }
    } else if (ParameterDeclaration.prototype.includes(op)) {
      isAlreadyFinal = JavaNode.getModifier(promisedFor, JavaNode.FINAL);
    }
    
    if (isAlreadyFinal) {
      context.reportError("Field/parameter is already declared to be final; no need to assume it", a);
      return null;
    } else {
      return new AssumeFinalPromiseDrop(a);
    }
  }
}
