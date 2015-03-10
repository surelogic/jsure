/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.aast.promise.VouchSpecificationNode;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.rules.LockRules.VouchFieldIs_ParseRule;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.VouchPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ClassBodyDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclList;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class VouchRules extends AnnotationRules {
  public static final String VOUCH = "Vouch";

  private static final AnnotationRules instance = new VouchRules();

  private static final Vouch_ParseRule vouchRule = new Vouch_ParseRule();

  public static AnnotationRules getInstance() {
    return instance;
  }

  public static VouchPromiseDrop getVouchSpec(IRNode decl) {
    return getDrop(vouchRule.getStorage(), decl);
  }

  /**
   * Returns the closest vouch applicable for the given IRNode, if any
   */
  public static VouchPromiseDrop getEnclosingVouch(final IRNode n) {
    IRNode decl = VisitUtil.getClosestDecl(n);
    while (decl != null) {
      Operator op = JJNode.tree.getOperator(decl);
      if (ClassBodyDeclaration.prototype.includes(op) || TypeDeclaration.prototype.includes(op) || 
    	  VariableDeclaration.prototype.includes(op)) {
        VouchPromiseDrop rv = getVouchSpec(decl);
        if (rv != null) {
          return rv;
        }
      }
      decl = VisitUtil.getEnclosingDecl(decl);
    }
    return null;
  }

  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, vouchRule);
  }

  static class Vouch_ParseRule extends DefaultSLAnnotationParseRule<VouchSpecificationNode, VouchPromiseDrop> {
    protected Vouch_ParseRule() {
      // Normally would use methodOrClassDeclOps, except for hack to handle
      // @Vouch("ThreadSafe")
      super(VOUCH, typeFuncVarDeclOps, VouchSpecificationNode.class);
    }

    @Override
    protected boolean producesOtherAASTRootNodes() {
    	return true;
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      if (context.getOp() instanceof VariableDeclList || context.getOp() instanceof ParameterDeclaration) {
        // Redirect to the appropriate rule
    	final VouchFieldIs_ParseRule rule = (VouchFieldIs_ParseRule) 
    		PromiseFramework.getInstance().getParseDropRule(LockRules.VOUCH_FIELD_IS);
    	try {
    		Object rv = rule.parse(context, initParser(context.getAllText()));
    		if (rv != null) {
    			return rv;
    		}
    	} catch(Exception e) {
    		// Ignore
    	}
   	    // Fall through to a normal vouch if it's not one of the special kinds that I know about
      }
      // Make sure there's no reason specified 
      final String reason = context.getProperty(VouchFieldIsNode.REASON);
      if (reason != null && reason.length() > 0) {
    	  context.reportError(0, "Normal vouches should not use the 'reason' element: "+context.getAllText());
    	  return null;
      }
      return new VouchSpecificationNode(context.mapToSource(0), context.getAllText());
    }

    @Override
    protected IPromiseDropStorage<VouchPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), VouchPromiseDrop.class);
    }

    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<VouchSpecificationNode, VouchPromiseDrop>(this) {
        @Override
        protected PromiseDrop<VouchSpecificationNode> makePromiseDrop(VouchSpecificationNode a) {
          VouchPromiseDrop d = new VouchPromiseDrop(a);
          return storeDropIfNotNull(a, d);
        }
      };
    }
  }
}
