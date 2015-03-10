package com.surelogic.annotation;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Interface for creating AASTs from text
 */
public interface IAnnotationParseRule<A extends IAASTRootNode, D extends PromiseDrop<? super A>> 
extends IPromiseRule, PromiseConstants {
  /** 
   * Returns the name of the annotation being unparsed.
   * Assumed to be a constant String
   */
  String name();
  
  /**
   * @return true if the operator 
   */
  boolean declaredOnValidOp(Operator op);
  
  /**
   * Create one or more AASTs on the AST node from the provided text.
   * All drops and/or errors are reported via the context.
   * 
   * @param n The AST node to be associated with the drops
   * @param contents The text to parse
   */
  ParseResult parse(IAnnotationParsingContext context, String contents);
  
  /**
   * @return null if there's no associated storage
   */
   IPromiseDropStorage<D> getStorage();
   
   /**
    * @return null if there's no associated scrubber
    */
   IAnnotationScrubber getScrubber();

   /**
    * A more detailed version of declaredOnValidOp
    */
   boolean appliesTo(IRNode decl, Operator op);
}
