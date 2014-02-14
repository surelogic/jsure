/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.Starts;
import com.surelogic.aast.promise.StartsSpecificationNode;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.method.constraints.StartsPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class ThreadEffectsRules extends AnnotationRules {
  public static final String STARTS = "Starts";
  
  private static final AnnotationRules instance = new ThreadEffectsRules();
  
  private static final Starts_ParseRule startsRule = new Starts_ParseRule();
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  public static boolean startsNothing(IRNode mdecl) {
    return getStartsSpec(mdecl) != null;
  }
  
  public static StartsPromiseDrop getStartsSpec(IRNode mdecl) {
    return getDrop(startsRule.getStorage(), mdecl);
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, startsRule);
  }
  
  static class Starts_ParseRule 
  extends DefaultSLAnnotationParseRule<StartsSpecificationNode,StartsPromiseDrop> {
    protected Starts_ParseRule() {
      super(STARTS, functionDeclOps, StartsSpecificationNode.class);
    }
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.starts().getTree();
    }
    
    @Override
    protected ProposedPromiseDrop.Builder proposeOnRecognitionException(IAnnotationParsingContext context, 
  		  String badContents, String okPrefix) {
      ProposedPromiseDrop.Builder p = context.startProposal(Starts.class);
      if (p == null) {
    	  return null;
      }
      return p.setValue("nothing").replaceSameExisting(badContents);
    }
    
    @Override
    protected IPromiseDropStorage<StartsPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), StartsPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<StartsSpecificationNode, StartsPromiseDrop>(this, ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
        @Override
        protected PromiseDrop<StartsSpecificationNode> makePromiseDrop(StartsSpecificationNode a) {
          /* Nothing to check here.  An annotated method may override an
           * unannotated or annotated method.  There is only one form of the
           * annotation.  Problem case is an unannotated method overriding
           * an annotated method.  That is checked elsewhere.
           */
          return storeDropIfNotNull(a, new StartsPromiseDrop(a));          
        }
        
        @Override
        protected boolean processUnannotatedMethodRelatedDecl(final IRNode decl) {
          /* If any of the immediate ancestors are annotated, then we have 
           * an error. 
           */
          boolean good = true;
          for (final IBinding pBinding : getContext().getBinder(decl).findOverriddenParentMethods(decl)) {
            final IRNode parent = pBinding.getNode();
            StartsPromiseDrop d = getStartsSpec(parent);
            if (d != null && !d.isAssumed()) {
              // Ancestor is annotated
              good = false;
              getContext().reportWarningAndProposal(
                  new Builder(Starts.class, decl, parent).setValue("nothing").setOrigin(Origin.PROBLEM).build(),
                  "Method must be annotated @Starts(\"nothing\") because it overrides @Starts(\"nothing\") {0}",
                  JavaNames.genRelativeFunctionName(parent));
            }
          }
          return good;
        }
      };
    }
  }
}
