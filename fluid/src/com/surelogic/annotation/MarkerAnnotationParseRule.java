package com.surelogic.annotation;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A parse rule that expects to parse nothing, and is not scrubbed 
 * 
 * @author Edwin.Chan
 */
public abstract class MarkerAnnotationParseRule<A extends IAASTRootNode, P extends PromiseDrop<A>>  
extends	DefaultBooleanAnnotationParseRule<A, P> {
	protected MarkerAnnotationParseRule(String name, Operator[] ops, Class<A> dt) {
		super(name, ops, dt);
	}
	@Override
	protected final Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
	    return parser.nothing().getTree();	  
	}
    @Override
    protected final IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<A, P>(this) {
        @Override
        protected PromiseDrop<A> makePromiseDrop(A a) {
          // We don't check anything here, since it'll be checked elsewhere.             
          P d = createDrop(a);
          return storeDropIfNotNull(a, d);          
        }
      };
    }
	protected abstract P createDrop(A a);
}
