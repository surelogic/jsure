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
import edu.cmu.cs.fluid.java.operator.ClassBodyDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class VouchRules extends AnnotationRules {
  public static final String VOUCH = "Vouch";
  
  private static final AnnotationRules instance = new VouchRules();
  
  private static final Vouch_ParseRule vouchRule = new Vouch_ParseRule();
 
  private static final VouchProcessor vouchProcessor = new VouchProcessor();
  
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
	  while (n != null) {		  
		  Operator op = JJNode.tree.getOperator(decl);
		  if (ClassBodyDeclaration.prototype.includes(op) || TypeDeclaration.prototype.includes(op)) {			 
			  VouchPromiseDrop rv = getVouchSpec(decl);
			  if (rv != null) {
				  return rv;
			  }
		  }
		  decl = VisitUtil.getEnclosingDecl(n);
	  }
	  return null;
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, vouchRule);
    Sea.getDefault().setProofInitializer(vouchProcessor);
  }
  
  static class VouchProcessor implements Sea.ProofInitializer {
	  public void init(Sea sea) {
		  for(ResultDrop d : sea.getDropsOfType(ResultDrop.class)) {
			  if (!d.isConsistent()) {
				  VouchPromiseDrop vouch = getEnclosingVouch(d.getNode());
				  if (vouch != null) {
					  // TODO what to do now
				  }
			  }
		  }
	  } 
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
