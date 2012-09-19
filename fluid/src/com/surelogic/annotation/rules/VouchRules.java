/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.VouchSpecificationNode;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.VouchPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ClassBodyDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
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
      if (ClassBodyDeclaration.prototype.includes(op) || TypeDeclaration.prototype.includes(op)) {
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
      super(VOUCH, fieldFuncTypeOps, VouchSpecificationNode.class);
    }

    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      if (context.getOp() instanceof FieldDeclaration) {
        // Redirect to the appropriate rule
        return PromiseFramework.getInstance().getParseDropRule(LockRules.VOUCH_FIELD_IS).parse(context, context.getAllText());
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
