/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.StartsSpecificationNode;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;

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
      super(STARTS, methodDeclOps, StartsSpecificationNode.class);
    }
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.starts().getTree();
    }
    @Override
    protected IPromiseDropStorage<StartsPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), StartsPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<StartsSpecificationNode> makeScrubber() {
      return new AbstractAASTScrubber<StartsSpecificationNode>(this, ScrubberType.BY_HIERARCHY) {
        @Override
        protected PromiseDrop<StartsSpecificationNode> makePromiseDrop(StartsSpecificationNode a) {
          return storeDropIfNotNull(getStorage(), a, scrubStarts(getContext(), a));          
        }
      };
    }
  }
  
  private static StartsPromiseDrop scrubStarts(
      final IAnnotationScrubberContext scrubberContext, 
      final StartsSpecificationNode a) {
//    // There are no syntactic checks, on consistency checks
//    // We only get here if the current method IS annotated!
//    final IRNode promisedFor = a.getPromisedFor();
//    for (final IBinding context : scrubberContext.getBinder().findOverriddenParentMethods(promisedFor)) {
//      final IRNode overriddenMethod = context.getNode();
//      
//    }
    
    return new StartsPromiseDrop(a);
  }
}
