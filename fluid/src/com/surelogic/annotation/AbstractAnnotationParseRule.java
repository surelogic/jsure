/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/AbstractAnnotationParseRule.java,v 1.15 2007/09/27 15:07:34 chance Exp $*/
package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.AbstractNamedPromiseRule;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Contains code common to most parse rules
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractAnnotationParseRule
<A extends IAASTRootNode, P extends PromiseDrop<? super A>> 
extends AbstractNamedPromiseRule 
implements ISingleAnnotationParseRule<A,P> {
  private final Class<A> aastType;
  private final IPromiseDropStorage<P> storage;
  private final IAnnotationScrubber scrubber;

  protected AbstractAnnotationParseRule(String name, Operator[] ops, Class<A> dt) {
    super(name, ops);
    aastType = dt;
    storage  = makeStorage();
    scrubber = makeScrubber();
  } 
  
  public final Class<A> getAASTType() {
    return aastType;
  }
  
  /*
  protected void reportAAST(IAnnotationParsingContext context, int offset, AnnotationLocation loc) {
    try {
      IAASTRootNode d = makeAAST(offset);
      context.reportAAST(offset, loc, d);
    } catch (Exception e) {
      context.reportException(offset, e);
    }
  }
  */
  
  /**
   * Returns whether or not the given Operator is one that this Promise can be
   * made on
   * 
   * @param declaredOn
   * @return
   */
  public final boolean declaredOnValidOp(Operator declaredOn) {
    for (Operator op : ops) {
      if (op.includes(declaredOn)) {
        return true;
      }
    }
    return false;
  }
  
  public final IPromiseDropStorage<P> getStorage() {
    return storage;
  }
  
  public final IAnnotationScrubber getScrubber() {
    return scrubber;
  }
  
  protected IPromiseDropStorage<P> makeStorage() {
    return null;
  }
  
  protected IAnnotationScrubber makeScrubber() {
    return null;
  }

  protected final void handleRecognitionException(IAnnotationParsingContext context, String contents,
      RecognitionException e) {
	  final ProposedPromiseDrop.Builder proposal;
	  if (e.charPositionInLine < contents.length()) {
		  if (e.charPositionInLine > 0) {
			  String ok  = contents.substring(0, e.charPositionInLine);
			  String bad = contents.substring(e.charPositionInLine);
			  proposal = proposeOnRecognitionException(context, contents, ok);
			  context.reportErrorAndProposal(e.charPositionInLine, 
					  "Unable to parse past @"+name()+' '+ok+" ___ "+bad,
					  proposal); 
		  } else {
			  proposal = proposeOnRecognitionException(context, contents, null);
			  context.reportErrorAndProposal(0, "Unable to parse: @"+name()+' '+contents, proposal); 
		  }
	  } else {
		  proposal = proposeOnRecognitionException(context, contents, null);
		  context.reportErrorAndProposal(IAnnotationParsingContext.UNKNOWN, 
				                         "Unable to parse: @"+name()+' '+contents, proposal);
	  }
  }

  /**
   * Check if there's a proposal to replace a bad annotation
   * 
   * @param badContents The unparseable contents
   */
  protected ProposedPromiseDrop.Builder proposeOnRecognitionException(IAnnotationParsingContext context, 
		  String badContents, String okPrefix) {
	  return null;
  }
  
  public boolean appliesTo(IRNode decl, Operator op) {
	  return true;
  }
}
