/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class VouchRules extends AnnotationRules {
  public static final String VOUCH = "Vouch";
  
  private static final AnnotationRules instance = new VouchRules();
  
  private static final Vouch_ParseRule vouchRule = new Vouch_ParseRule();
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  public static VouchPromiseDrop getVouchSpec(IRNode mdecl) {
    return getDrop(vouchRule.getStorage(), mdecl);
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, vouchRule);
  }
  
  static class Vouch_ParseRule 
  extends DefaultSLAnnotationParseRule<VouchSpecificationNode, VouchPromiseDrop> {
    protected Vouch_ParseRule() {
      super(VOUCH, methodOrClassDeclOps, VouchSpecificationNode.class);
    }
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return new VouchSpecificationNode(context.mapToSource(0), context.getAllText());
    }
    @Override
    protected IPromiseDropStorage<VouchPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), VouchPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<VouchSpecificationNode> makeScrubber() {
      return new AbstractAASTScrubber<VouchSpecificationNode>(this) {
        @Override
        protected PromiseDrop<VouchSpecificationNode> makePromiseDrop(VouchSpecificationNode a) {
          VouchPromiseDrop d = new VouchPromiseDrop(a);
          return storeDropIfNotNull(getStorage(), a, d);          
        }
      };
    }
  }
}
